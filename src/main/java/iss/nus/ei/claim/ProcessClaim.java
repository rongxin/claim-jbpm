package iss.nus.ei.claim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jbpm.test.JBPMHelper;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;

public class ProcessClaim {

	public static void main(String[] args) {
		KieServices ks = KieServices.Factory.get();
		KieContainer kContainer = ks.getKieClasspathContainer();
		KieBase kbase = kContainer.getKieBase("kbase");

		RuntimeManager manager = createRuntimeManager(kbase);
		RuntimeEngine engine = manager.getRuntimeEngine(null);
		KieSession ksession = engine.getKieSession();
		TaskService taskService = engine.getTaskService();

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("managerApproval", true);

		ksession.startProcess("claim.bpmn.claim_process", params);

		// let employee(john) execute Task 1
		List<TaskSummary> johnTaskList = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");

		TaskSummary task = johnTaskList.get(0);
		System.out.println("John [Employee] is executing task " + task.getName());
		taskService.start(task.getId(), "john");
		taskService.complete(task.getId(), "john", null);

		// let manager(mary) execute Task 2
		List<TaskSummary> maryTaskList = taskService.getTasksAssignedAsPotentialOwner("mary", "en-UK");

		TaskSummary maryTask = maryTaskList.get(0);
		System.out.println("Mary [Manager] is executing task " + maryTask.getName());
		taskService.start(maryTask.getId(), "mary");

		Map<String, Object> results = new HashMap<String, Object>();
		results.put("managerApproval", false);

		taskService.complete(maryTask.getId(), "mary", results);

		// let finance department(krisv) execute Task 3
		List<TaskSummary> krisvTaskList = taskService.getTasksAssignedAsPotentialOwner("krisv", "en-UK");

		if (!krisvTaskList.isEmpty()) {
			TaskSummary krisvTask = krisvTaskList.get(0);
			System.out.println("Krisv [Finance department] is executing task " + krisvTask.getName());
			taskService.start(krisvTask.getId(), "krisv");

			taskService.complete(krisvTask.getId(), "krisv", null);
		}

		manager.disposeRuntimeEngine(engine);
		System.exit(0);
	}

	private static RuntimeManager createRuntimeManager(KieBase kbase) {
		JBPMHelper.startH2Server();
		JBPMHelper.setupDataSource();
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
		RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory.get().newDefaultBuilder()
				.entityManagerFactory(emf).knowledgeBase(kbase);
		return RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(builder.get(),
				"iss.nus.ei:claim-jbpm:1.0");
	}

}