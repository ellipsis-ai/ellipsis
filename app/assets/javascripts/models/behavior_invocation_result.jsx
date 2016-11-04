define(function() {

  class BehaviorInvocationTestResult {
    constructor(response, missingParamNames) {
      this.response = response || '';
      this.missingParamNames = missingParamNames || [];
    }
  }

  return BehaviorInvocationTestResult;
});
