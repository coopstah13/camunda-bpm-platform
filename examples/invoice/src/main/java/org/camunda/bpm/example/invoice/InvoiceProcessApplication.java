/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.example.invoice;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;

import static org.camunda.bpm.engine.variable.Variables.createVariables;
import static org.camunda.bpm.engine.variable.Variables.fileValue;

import org.camunda.bpm.application.PostDeploy;
import org.camunda.bpm.application.ProcessApplication;
import org.camunda.bpm.application.impl.ServletProcessApplication;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.authorization.Groups;
import org.camunda.bpm.engine.impl.util.ClockUtil;
import org.camunda.bpm.engine.impl.util.IoUtil;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;

/**
 * Process Application exposing this application's resources the process engine.
 */
@ProcessApplication
public class InvoiceProcessApplication extends ServletProcessApplication {

  /**
   * In a @PostDeploy Hook you can interact with the process engine and access
   * the processes the application has deployed.
   */
  @PostDeploy
  public void startFirstProcess(ProcessEngine processEngine) {
    createUsers(processEngine);

    deployProcessDefinitions(processEngine);
    startProcessInstances(processEngine, "invoice", 1);
    startProcessInstances(processEngine, "invoice", 2);
  }

  private void deployProcessDefinitions(ProcessEngine processEngine) {
    RepositoryService repositoryService = processEngine.getRepositoryService();
    if (repositoryService.createProcessDefinitionQuery().processDefinitionKey("invoice").count() == 0) {
      ClassLoader classLoader = getProcessApplicationClassloader();

      // deploy version 1
      repositoryService.createDeployment()
        .addInputStream("invoice.bpmn", classLoader.getResourceAsStream("invoice.v1.bpmn"))
        .addInputStream("assign-approver-groups.dmn", classLoader.getResourceAsStream("assign-approver-groups.dmn"))
        .deploy();

      // deploy version 2
      repositoryService.createDeployment()
        .addInputStream("invoice.bpmn", classLoader.getResourceAsStream("invoice.v2.bpmn"))
        .addInputStream("assign-approver-groups.dmn", classLoader.getResourceAsStream("assign-approver-groups.dmn"))
        .deploy();
    }

  }

  private void startProcessInstances(ProcessEngine processEngine, String processDefinitionKey, int processDefinitionVersion) {

    InputStream invoiceInputStream = getProcessApplicationClassloader().getResourceAsStream("invoice.pdf");
    String processDefinitionId = processEngine.getRepositoryService().createProcessDefinitionQuery()
      .processDefinitionKey(processDefinitionKey)
      .processDefinitionVersion(processDefinitionVersion)
      .singleResult()
      .getId();

    // process instance 1
    processEngine.getRuntimeService().startProcessInstanceById(processDefinitionId, createVariables()
        .putValue("creditor", "Great Pizza for Everyone Inc.")
        .putValue("amount", 30.00d)
        .putValue("invoiceCategory", "Travel Expenses")
        .putValue("invoiceNumber", "GPFE-23232323")
        .putValue("invoiceDocument", fileValue("invoice.pdf")
            .file(invoiceInputStream)
            .mimeType("application/pdf")
            .create()));

    IoUtil.closeSilently(invoiceInputStream);
    invoiceInputStream = InvoiceProcessApplication.class.getClassLoader().getResourceAsStream("invoice.pdf");

    // process instance 2
    try {
      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.DAY_OF_MONTH, -14);
      ClockUtil.setCurrentTime(calendar.getTime());

      ProcessInstance pi = processEngine.getRuntimeService().startProcessInstanceById(processDefinitionId, createVariables()
          .putValue("creditor", "Bobby's Office Supplies")
          .putValue("amount", 900.00d)
          .putValue("invoiceCategory", "Misc")
          .putValue("invoiceNumber", "BOS-43934")
          .putValue("invoiceDocument", fileValue("invoice.pdf")
              .file(invoiceInputStream)
              .mimeType("application/pdf")
              .create()));

      calendar.add(Calendar.DAY_OF_MONTH, 14);
      ClockUtil.setCurrentTime(calendar.getTime());

      processEngine.getIdentityService().setAuthentication("demo", Arrays.asList(Groups.CAMUNDA_ADMIN));
      Task task = processEngine.getTaskService().createTaskQuery().processInstanceId(pi.getId()).singleResult();
      processEngine.getTaskService().claim(task.getId(), "demo");
      processEngine.getTaskService().complete(task.getId(), createVariables().putValue("approved", true));
    }
    finally{
      ClockUtil.reset();
      processEngine.getIdentityService().clearAuthentication();
    }

    IoUtil.closeSilently(invoiceInputStream);
    invoiceInputStream = InvoiceProcessApplication.class.getClassLoader().getResourceAsStream("invoice.pdf");

    // process instance 3
    try {
      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.DAY_OF_MONTH, -5);
      ClockUtil.setCurrentTime(calendar.getTime());

      ProcessInstance pi = processEngine.getRuntimeService().startProcessInstanceById(processDefinitionId, createVariables()
          .putValue("creditor", "Papa Steve's all you can eat")
          .putValue("amount", 10.99d)
          .putValue("invoiceCategory", "Travel Expenses")
          .putValue("invoiceNumber", "PSACE-5342")
          .putValue("invoiceDocument", fileValue("invoice.pdf")
              .file(invoiceInputStream)
              .mimeType("application/pdf")
              .create()));

      calendar.add(Calendar.DAY_OF_MONTH, 5);
      ClockUtil.setCurrentTime(calendar.getTime());

      processEngine.getIdentityService().setAuthenticatedUserId("mary");
      Task task = processEngine.getTaskService().createTaskQuery().processInstanceId(pi.getId()).singleResult();
      processEngine.getTaskService().createComment(null, pi.getId(), "I cannot approve this invoice: the amount is missing.\n\n Could you please provide the amount?");
      processEngine.getTaskService().complete(task.getId(), createVariables().putValue("approved", "false"));
    }
    finally{
      ClockUtil.reset();
      processEngine.getIdentityService().clearAuthentication();
    }
  }

  private void createUsers(ProcessEngine processEngine) {

    // create demo users
    new DemoDataGenerator().createUsers(processEngine);
  }
}
