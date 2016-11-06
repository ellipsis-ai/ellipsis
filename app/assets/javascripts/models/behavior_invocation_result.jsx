define(function() {

  class BehaviorInvocationTestResult {
    constructor(response, missingParamNames, missingUserEnvVars) {
      this.response = response || '';
      this.missingParamNames = missingParamNames || [];
      this.missingUserEnvVars = missingUserEnvVars || [];
    }
  }

  return BehaviorInvocationTestResult;
});
