define(function() {

  class BehaviorInvocationTestResult {
    constructor(props) {
      this.responseText = props.responseText || "";
      this.kind = props.kind || null;
      this.missingInputNames = props.missingInputNames || [];
      this.missingSimpleTokens = props.missingSimpleTokens || [];
      this.missingUserEnvVars = props.missingUserEnvVars || [];
      this.files = props.files || [];
    }

    wasSuccessful() {
      return this.kind === "Success";
    }

    wasNoResponse() {
      return this.kind === "NoResponse";
    }

    static fromReportJSON(props) {
      return new BehaviorInvocationTestResult({
        responseText: props.result && props.result.fullText ? props.result.fullText : "",
        kind: props.result && props.result.kind ? props.result.kind : null,
        missingInputNames: props.missingInputNames || [],
        missingSimpleTokens: props.missingSimpleTokens || [],
        missingUserEnvVars: props.missingUserEnvVars || [],
        files: props.result && props.result.files ? props.result.files : []
      });
    }
  }

  return BehaviorInvocationTestResult;
});
