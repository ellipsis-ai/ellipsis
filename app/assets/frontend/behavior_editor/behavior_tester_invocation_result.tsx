import * as React from 'react';
import InvocationTestResult from '../models/behavior_invocation_result';
import BehaviorTesterInvocationResultFile from './behavior_tester_invocation_result_file';

interface Props {
  result: InvocationTestResult
}

class BehaviorTesterInvocationResult extends React.Component<Props> {
    containerClassNames(): string {
      const result = this.props.result;
      if (result.wasSuccessful()) {
        return "border-green";
      } else if (result.wasNoResponse()) {
        return "";
      } else {
        return "border-pink";
      }
    }

    missingSimpleTokens() {
      const missingSimpleTokens = this.props.result.missingSimpleTokens;
      return (
        <div>
          {missingSimpleTokens.length === 1 ? (
            <span>
              If, like you, the user hasn’t yet supplied a token for the <code className="type-bold">{missingSimpleTokens[0]}</code> API, Ellipsis will ask for one.
            </span>
          ) : (
            <span>
              <span>If, like you, the user hasn’t yet supplied tokens for these APIs, Ellipsis will ask for them: </span>
              <code className="type-bold mlxs">{missingSimpleTokens.join(", ")}</code>
            </span>
          )}
        </div>
      );
    }

    missingInputs() {
      const missingInputNames = this.props.result.missingInputNames;
      return (
        <div>
          {missingInputNames.length === 1 ? (
            <span>
              Ellipsis will ask the user for a value for the input <code className="type-bold mlxs">{missingInputNames[0]}</code>.
            </span>
          ) : (
            <span>
              <span>Ellipsis will ask the user for values for these inputs: </span>
              <code className="type-bold mlxs">{missingInputNames.join(", ")}</code>
            </span>
          )}
        </div>
      );
    }

    renderText() {
      const result = this.props.result;
      if (result.responseText) {
        return (
          <pre>{result.responseText}</pre>
        );
      } else if (result.wasNoResponse()) {
        return (
          <i>The action completed, but sent no response.</i>
        );
      } else {
        return (
          <div>
            {result.missingSimpleTokens.length > 0 ? this.missingSimpleTokens() : null}
            {result.missingInputNames.length > 0 ? this.missingInputs() : null}
          </div>
        );
      }
    }

    renderFiles() {
      const files = this.props.result.files;
      if (files && files.length > 0) {
        return (
          <div>
            {files.map((file, index) => (
              <BehaviorTesterInvocationResultFile key={`file${index}`}
                filename={file.filename}
                filetype={file.filetype}
                content={file.content}
              />
            ))}
          </div>
        );
      } else {
        return null;
      }
    }

    render() {
      return (
        <div
          className={
            `display-overflow-scroll border pas bg-white ${
              this.containerClassNames()
            }`}
        >
          {this.renderText()}
          {this.renderFiles()}
        </div>
      );
    }
}

export default BehaviorTesterInvocationResult;

