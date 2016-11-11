define(function() {

  class BehaviorInvocationTestResult {
    constructor(response, missingParamNames, missingSimpleTokens, missingUserEnvVars) {
      this.response = response || '';
      this.missingParamNames = missingParamNames || [];
      this.missingSimpleTokens = missingSimpleTokens || [];
      this.missingUserEnvVars = missingUserEnvVars || [];
    }
  }

  return BehaviorInvocationTestResult;
});
