// @flow
define(function() {

  class BehaviorInvocationTestResult {
    responseText: string;
    kind: ?string;
    missingInputNames: Array<string>;
    missingSimpleTokens: Array<string>;
    files: Array<string>;

    constructor(
      responseText: ?string,
      kind: ?string,
      missingInputNames: ?Array<string>,
      missingSimpleTokens: ?Array<string>,
      files: Array<string>
    ) {
      Object.defineProperties(this, {
        responseText: { value: responseText || "", enumerable: true },
        kind: { value: kind, enumerable: true },
        missingInputNames: { value: missingInputNames || [], enumerable: true },
        missingSimpleTokens: { value: missingSimpleTokens || [], enumerable: true },
        files: { value: files || [], enumerable: true }
      });
    }

    wasSuccessful(): boolean {
      return this.kind === "Success";
    }

    wasNoResponse(): boolean {
      return this.kind === "NoResponse";
    }

    static fromProps(props): BehaviorInvocationTestResult {
      return new BehaviorInvocationTestResult(
        props.responseText,
        props.kind,
        props.missingInputNames,
        props.missingSimpleTokens,
        props.files
      );
    }

    static fromReportJSON(props): BehaviorInvocationTestResult {
      return BehaviorInvocationTestResult.fromProps({
        responseText: props.result && props.result.fullText ? props.result.fullText : "",
        kind: props.result && props.result.kind ? props.result.kind : null,
        missingInputNames: props.missingInputNames || [],
        missingSimpleTokens: props.missingSimpleTokens || [],
        files: props.result && props.result.files ? props.result.files : []
      });
    }
  }

  return BehaviorInvocationTestResult;
});
