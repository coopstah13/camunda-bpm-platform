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
package org.camunda.bpm.qa.upgrade.scenarios.sentry;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.qa.upgrade.DescribesScenario;
import org.camunda.bpm.qa.upgrade.ScenarioSetup;

/**
 * Just for testing
 *
 * @author Thorben Lindhauer
 *
 */
public class OneTaskProcessScenario {

  @Deployment
  public static String deployOneTaskProcess() {
    return "org/camunda/bpm/qa/upgrade/oneTaskProcess.bpmn20.xml";
  }

  @DescribesScenario("simpleInstance")
  public static ScenarioSetup triggerEntryCriterion() {
    return new ScenarioSetup() {
      public void execute(ProcessEngine engine, String scenarioName) {
        RuntimeService runtimeService = engine.getRuntimeService();
        runtimeService.startProcessInstanceByKey("oneTaskProcess", scenarioName);
      }
    };
  }

}