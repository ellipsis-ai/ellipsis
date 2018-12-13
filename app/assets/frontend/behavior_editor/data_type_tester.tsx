import * as React from 'react';
import {testInvocation} from './behavior_test';
import FormInput from '../form/input';
import Collapsible from '../shared_ui/collapsible';
import {RequiredOAuthApplication} from '../models/oauth';
import TesterAuthRequired from './tester_auth_required';
import InvocationResults from './behavior_tester_invocation_results';
import InvocationTestResult, {BehaviorInvocationTestReportOutput} from '../models/behavior_invocation_result';
import BehaviorTesterInvocationResultFile from './behavior_tester_invocation_result_file';
import autobind from "../lib/autobind";

const MAX_RESULTS_TO_SHOW = 10;

interface Props {
  groupId: string,
  behaviorId: string,
  isSearch?: Option<boolean>,
  csrfToken: string,
  onDone: () => void,
  appsRequiringAuth: Array<RequiredOAuthApplication>
}

interface State {
  searchQuery: string,
  results: Array<InvocationTestResult>,
  isTesting: boolean,
  hasTested: boolean
}

interface DataTypeChoice {
  label: string
  id: string
  [k: string]: unknown
}

class DataTypeTester extends React.Component<Props, State> {
    searchQueryInput: Option<FormInput>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        searchQuery: '',
        results: [],
        isTesting: false,
        hasTested: false
      };
    }

    getResults(): Array<InvocationTestResult> {
      return this.state.results;
    }

    isStringArray(obj: unknown): obj is Array<string> {
      return Array.isArray(obj) && obj.every(ea => typeof ea === "string");
    }

    isValidDataTypeOption(obj: any): obj is DataTypeChoice {
      return obj && typeof obj.label === "string" && typeof obj.id === "string";
    }

    isValidDataTypeArray(obj: unknown): obj is Array<DataTypeChoice> {
      return Array.isArray(obj) && obj.every(this.isValidDataTypeOption);
    }

    getParsedResponse(result: InvocationTestResult): Option<Array<DataTypeChoice>> {
      try {
        const parsed = JSON.parse(result.responseText);
        if (this.isStringArray(parsed)) {
          return parsed.map((ea) => {
            return { label: ea, id: ea };
          });
        } else if (this.isValidDataTypeArray(parsed)) {
          return parsed;
        } else {
          return null;
        }
      } catch(e) {
        return null;
      }
    }

    focus() {
      if (this.searchQueryInput) {
        this.searchQueryInput.focus();
      }
    }

    onChangeSearchQuery(value: string): void {
      this.setState({
        searchQuery: value
      });
    }

    onClick() {
      this.updateResult();
    }

    onEnterKey() {
      if (this.searchQueryInput) {
        this.searchQueryInput.blur();
      }
      if (!this.state.isTesting) {
        this.updateResult();
      }
    }

    onDone() {
      this.props.onDone();
    }

    updateResult() {
      this.setState({
        hasTested: true,
        isTesting: true
      }, this.fetchResult);
    }

    params(): { [name: string]: string } {
      if (this.state.searchQuery) {
        return { searchQuery: this.state.searchQuery };
      } else {
        return {};
      }
    }

    fetchResult() {
      testInvocation({
        behaviorId: this.props.behaviorId,
        csrfToken: this.props.csrfToken,
        paramValues: this.params(),
        onSuccess: (json: BehaviorInvocationTestReportOutput) => {
          var newResults = this.state.results.concat(InvocationTestResult.fromReportJSON(json));
          this.setState({
            results: newResults,
            isTesting: false
          });
        },
        onError: () => {
          // TODO: Some error handling would be nice
          this.setState({
            isTesting: false
          });
        }
      });
    }

    render() {
      return (
        <div>
          <Collapsible revealWhen={this.state.hasTested}>
            <InvocationResults
              results={this.getResults()}
              resultStatus={this.renderResultStatus()}
              onRenderResult={this.renderResult}
            />
          </Collapsible>
          <div className="box-action">
            <div className="container phn">
              <div className="columns mtl">
                <div className="column column-page-sidebar">
                  <h4 className="mtn type-weak">Test the data type</h4>
                </div>
                <div className="column column-three-quarters pll mobile-pln mobile-column-full">
                  {this.renderContent()}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }

    renderContent() {
      if (this.props.behaviorId && this.props.appsRequiringAuth.length > 0) {
        return (
          <TesterAuthRequired
            groupId={this.props.groupId}
            behaviorId={this.props.behaviorId}
            appsRequiringAuth={this.props.appsRequiringAuth}
          />
        );
      } else {
        return this.renderTester();
      }
    }

    renderSearchQuery() {
      if (this.props.isSearch) {
        return (
          <FormInput
            className="width-20 mrs mbs"
            placeholder="Search query"
            ref={(el) => this.searchQueryInput = el}
            value={this.state.searchQuery}
            onChange={this.onChangeSearchQuery}
            onEnterKey={this.onEnterKey}
          />
        );
      } else {
        return null;
      }
    }

    renderResultStatus() {
      const result = this.getResults()[this.getResults().length - 1];
      const parsedResult = result ? this.getParsedResponse(result) : null;
      if (this.state.isTesting) {
        return (
          <span className="type-weak pulse">Testing</span>
        );
      } else if (parsedResult) {
        const numMatches = parsedResult.length;
        return (
          <span className="type-green">Last response valid {numMatches === 1 ? '(1 item)' : `(${numMatches} items)`} ✓</span>
        );
      } else if (result) {
        return (
          <span className="type-pink">
            <span>Last response invalid: must call <code>ellipsis.success</code> with an array of objects, </span>
            <span>each with an <code className="type-black">id</code> and <code className="type-black">label</code> property.</span>
          </span>
        );
      } else {
        return (
          <span className="type-weak">Last response missing</span>
        );
      }
    }

    renderResult(result: InvocationTestResult) {
      const parsedResponse = this.getParsedResponse(result);
      if (parsedResponse) {
        return this.renderValidResultTableWith(result, parsedResponse);
      } else {
        return (
          <div className="display-overflow-scroll border border-pink bg-white pas">
            {result.responseText ? (
              <pre>{result.responseText}</pre>
            ) : (
              <i>{"(No response occurred.)"}</i>
            )}
          </div>
        );
      }
    }

    renderFiles(result: InvocationTestResult) {
      return (
        <div>
          {result.files.map((file, index) => (
            <BehaviorTesterInvocationResultFile key={`file${index}`}
                                                filename={file.filename}
                                                filetype={file.filetype}
                                                content={file.content}
            />
          ))}
        </div>
      );
    }

    renderValidResultTableWith(result: InvocationTestResult, response: Array<DataTypeChoice>) {
      const hasOtherData = response.some((item) => {
        return Object.keys(item).filter((key) => {
          return key !== 'id' && key !== 'label';
        }).length > 0;
      });
      const overflowResults = response.length - MAX_RESULTS_TO_SHOW;
      if (response.length > 0) {
        return (
          <div className="border pas border-green bg-white">
            <div className="columns columns-elastic">
              <div className="column-group">
                <div className="column-row type-s type-monospace">
                  <div className="column column-shrink pbxs">id</div>
                  <div className="column column-shrink pbxs">label</div>
                  <div className="column column-expand pbxs">
                    {hasOtherData ? "Other properties" : ""}
                  </div>
                </div>
                {response.slice(0, MAX_RESULTS_TO_SHOW).map((item, itemIndex) => (
                  <div className="column-row" key={`item${itemIndex}`}>
                    <div className="column column-shrink pbxs">
                      <pre className="box-code-example display-inline-block">{item.id}</pre>
                    </div>
                    <div className="column column-shrink pbxs">
                      <pre className="box-code-example display-inline-block">{item.label}</pre>
                    </div>
                    <div className="column column-expand pbxs">
                      {hasOtherData ? this.renderOtherDataForItem(item) : null}
                    </div>
                  </div>
                ))}
              </div>
            </div>
            {overflowResults > 0 ? (
              <div className="ptxs type-italic type-s type-weak">
                {overflowResults === 1 ?
                  "1 more item not shown" :
                  `${overflowResults} more items not shown`
                }
              </div>
            ) : null}
            {this.renderFiles(result)}
          </div>
        );
      } else {
        return (
          <div className="border pas border-green bg-white">
            <div className="type-italic">An empty list was returned.</div>
            {this.renderFiles(result)}
          </div>
        );
      }
    }

    renderOtherDataForItem(item: DataTypeChoice) {
      const copy = Object.assign({}, item);
      delete copy.id;
      delete copy.label;
      const asString = JSON.stringify(copy, null, 1);
      return (
        <div className="box-code-example display-limit-width display-ellipsis type-monospace"
          title={asString}
        >{asString}</div>
      );
    }

    renderIntro() {
      if (this.props.isSearch) {
        return (
          <p>
            Type a search query, then click Test to run your code and check the result.
          </p>
        );
      } else {
        return (
          <p>
            Click Test to run your code and check the result.
          </p>
        );
      }
    }

    renderTester() {
      return (
        <div>
          <div className="mbxl">
            {this.renderIntro()}
            <div className="columns columns-elastic">
              <div className="column column-expand">
                {this.renderSearchQuery()}
                <button className="button-primary mbs" type="button"
                  onClick={this.onClick}
                  disabled={this.state.isTesting || Boolean(this.props.isSearch && !this.state.searchQuery)}
                >Test</button>
              </div>
              <div className="column column-shrink align-b">
                <button className="mbs" type="button" onClick={this.onDone}>Done</button>
              </div>
            </div>
          </div>

        </div>
      );
    }
}

export default DataTypeTester;
