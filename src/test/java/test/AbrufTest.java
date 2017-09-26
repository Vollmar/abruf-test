package test;

import org.apache.ibatis.logging.LogFactory;
import org.camunda.bpm.engine.runtime.CaseExecution;
import org.camunda.bpm.engine.runtime.CaseInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.init;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.processEngine;
import static org.camunda.bpm.engine.test.assertions.cmmn.CmmnAwareTests.*;

public class AbrufTest {


	@ClassRule
	@Rule
	public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

	private static final String PROCESS_DEFINITION_KEY = "abruf";

	static {
		LogFactory.useSlf4jLogging(); // MyBatis
	}

	@Before
	public void setup() {
		init(rule.getProcessEngine());
	}

	/**
	 * Just tests if the process definition is deployable.
	 */
	@Test
	@Deployment(resources = "abruf.cmmn")
	public void testParsingAndDeployment() {
		// nothing is done here, as we just want to check for exceptions during deployment
	}

	@Test
	@Deployment(resources = "abruf.cmmn")
	public void testHappyPath() throws SQLException {

		final CaseInstance caseInstance = caseService().createCaseInstanceByKey(PROCESS_DEFINITION_KEY);

		assertThat(caseInstance).isActive();

		final CaseExecution kunde = caseExecution("KundeId", caseInstance);
		assertThat(kunde).isHumanTask();
		manuallyStart(kunde);
		final Task kundeTask = taskService().createTaskQuery().taskCandidateUser("kunde").singleResult();


		complete(kundeTask,withVariables("databaseId","443535"));


		final CaseExecution kunde2 = caseExecution("KundeId", caseInstance);
		assertThat(kunde2).isHumanTask();
		manuallyStart(kunde2);
		final Task kunde2Task = taskService().createTaskQuery().taskCandidateUser("kunde").singleResult();

		complete(kunde2Task,withVariables("databaseId","newId"));



		final List<Task> sachbearbeiterList = taskService().createTaskQuery().taskCandidateUser("sachbearbeiter").list();

		assertThat(sachbearbeiterList.size()).isEqualTo(2);
		final Task task = sachbearbeiterList.get(0); //sollte 443535
		final Task task2 = sachbearbeiterList.get(1); // sollte newId
		assertThat(task).isNotEqualTo(task2);

		assertThat(taskService().getVariable(task.getId(),"databaseId")).isEqualTo("443535");

		assertThat(taskService().getVariable(task2.getId(),"databaseId")).isEqualTo("newId");

	}
}
