import {DataRequest} from "../lib/data_request";
import {BehaviorInvocationTestReportOutput} from "../models/behavior_invocation_result";

function testInvocationUrl() {
  return jsRoutes.controllers.BehaviorEditorController.testInvocation().url;
}

function testTriggersUrl() {
  return jsRoutes.controllers.BehaviorEditorController.testTriggers().url;
}

interface TestParams {
  behaviorId: string
  csrfToken: string
  onError: () => void
}

interface InvocationTestParams extends TestParams {
  paramValues: {
    [name: string]: string
  }
  onSuccess: (output: BehaviorInvocationTestReportOutput) => void
}

interface BehaviorTriggerTestReportOutput {
  message: string
  activatedTrigger: Option<string>
  paramValues: {
    [name: string]: string
  }
}

interface TriggerTestParams extends TestParams {
  message: string
  onSuccess: (output: BehaviorTriggerTestReportOutput) => void
}

function testInvocation(params: InvocationTestParams): void {
  DataRequest.jsonPost(testInvocationUrl(), {
    behaviorId: params.behaviorId,
    paramValuesJson: JSON.stringify(params.paramValues)
  }, params.csrfToken)
    .then(params.onSuccess)
    .catch(params.onError);
}

function testTriggers(params: TriggerTestParams) {
  DataRequest.jsonPost(testTriggersUrl(), {
    behaviorId: params.behaviorId,
    message: params.message
  }, params.csrfToken)
    .then(params.onSuccess)
    .catch(params.onError);
}

export {testInvocation, testTriggers};
