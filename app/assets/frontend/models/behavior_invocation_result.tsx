interface BehaviorInvocationResultFile {
  content: string | null,
  filetype: string | null,
  filename: string | null
}

interface BehaviorInvocationResultOutput {
  kind: string,
  fullText: string,
  files: Array<BehaviorInvocationResultFile>
}

interface BehaviorInvocationTestReportOutput {
  missingInputNames: Array<string>,
  missingSimpleTokens: Array<string>,
  result: BehaviorInvocationResultOutput | null
}

export interface BehaviorInvocationTestResultJson {
  responseText: string,
  kind: string | null,
  missingInputNames: Array<string>,
  missingSimpleTokens: Array<string>,
  files: Array<BehaviorInvocationResultFile>
}

class BehaviorInvocationTestResult implements BehaviorInvocationTestResultJson {
  readonly responseText: string;

  constructor(
    responseText: string | null,
    readonly kind: string | null,
    readonly missingInputNames: Array<string>,
    readonly missingSimpleTokens: Array<string>,
    readonly files: Array<BehaviorInvocationResultFile>
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

    static fromProps(props: BehaviorInvocationTestResultJson): BehaviorInvocationTestResult {
      return new BehaviorInvocationTestResult(
        props.responseText,
        props.kind,
        props.missingInputNames,
        props.missingSimpleTokens,
        props.files
      );
    }

    static fromReportJSON(props: BehaviorInvocationTestReportOutput): BehaviorInvocationTestResult {
      return BehaviorInvocationTestResult.fromProps({
        responseText: props.result && props.result.fullText ? props.result.fullText : "",
        kind: props.result && props.result.kind ? props.result.kind : null,
        missingInputNames: props.missingInputNames || [],
        missingSimpleTokens: props.missingSimpleTokens || [],
        files: props.result && props.result.files ? props.result.files : []
      });
    }
}

export default BehaviorInvocationTestResult;

