define(function() {

  class BehaviorInvocationTestResult {
    constructor(response, missingInputNames, missingSimpleTokens, missingUserEnvVars) {
      this.response = response || '';
      this.missingInputNames = missingInputNames || [];
      this.missingSimpleTokens = missingSimpleTokens || [];
      this.missingUserEnvVars = missingUserEnvVars || [];
    }
  }

  return BehaviorInvocationTestResult;
});
