import * as React from 'react';
import {testTriggers, testInvocation} from './behavior_test';
import Collapsible from '../shared_ui/collapsible';
import DynamicLabelButton from '../form/dynamic_label_button';
import FormInput from '../form/input';
import Input from '../models/input';
import Trigger from '../models/trigger';
import * as debounce from 'javascript-debounce';
import {RequiredOAuthApplication} from '../models/oauth';
import TesterAuthRequired from './tester_auth_required';
import InvocationTestResult, {BehaviorInvocationTestReportOutput} from '../models/behavior_invocation_result';
import InvocationResults from './behavior_tester_invocation_results';
import autobind from "../lib/autobind";

interface Props {
  triggers: Array<Trigger>,
  inputs: Array<Input>,
  groupId: string,
  behaviorId: string,
  csrfToken: string,
  onDone: () => void,
  appsRequiringAuth: Array<RequiredOAuthApplication>
}

interface State {
  testMessage: string,
  highlightedTriggerText: Option<string>,
  inputValues: {
    [name: string]: string
  },
  isTestingTriggers: boolean,
  isTestingResult: boolean,
  hasTestedTriggers: boolean,
  hasTestedResult: boolean,
  triggerErrorOccurred: boolean,
  resultErrorOccurred: boolean,
  resultMissingInputNames: Array<string>,
  results: Array<InvocationTestResult>
}

class BehaviorTester extends React.Component<Props, State> {
  testMessage: Option<FormInput>;
  validateMessage: () => void;

  constructor(props) {
    super(props);
    autobind(this);
    this.state = {
      testMessage: '',
      highlightedTriggerText: null,
      inputValues: {},
      isTestingTriggers: false,
      isTestingResult: false,
      hasTestedTriggers: false,
      hasTestedResult: false,
      triggerErrorOccurred: false,
      resultErrorOccurred: false,
      resultMissingInputNames: [],
      results: []
    };
    this.validateMessage = debounce(this._validateMessage, 500);
  }

    componentWillReceiveProps(newProps: Props): void {
      if (JSON.stringify(this.props.triggers) !== JSON.stringify(newProps.triggers)) {
        this.setState({
          testMessage: '',
          highlightedTriggerText: null,
          isTestingTriggers: false,
          isTestingResult: false,
          hasTestedTriggers: false,
          triggerErrorOccurred: false
        });
      }
      if (this.props.inputs.some((input, index) => {
        return !newProps.inputs[index] ||
          !input.isSameNameAndTypeAs(newProps.inputs[index]);
      })) {
        this.setState({
          inputValues: {}
        });
      }
    }

    onChangeTestMessage(value: string): void {
      this.setState({
        testMessage: value,
        highlightedTriggerText: null,
        inputValues: {},
        hasTestedTriggers: false,
        triggerErrorOccurred: false,
        resultErrorOccurred: false
      });
      if (value) {
        this.validateMessage();
      }
    }

    onChangeInputValue(name: string, value: string): void {
      var newInputValues = Object.assign({}, this.state.inputValues);
      newInputValues[name] = value;
      this.setState({
        inputValues: newInputValues
      });
    }

    onDone(): void {
      this.props.onDone();
    }

    _validateMessage(): void {
      this.setState({
        isTestingTriggers: true
      }, this.sendValidationRequest);
    }

    sendValidationRequest(): void {
      testTriggers({
        behaviorId: this.props.behaviorId,
        csrfToken: this.props.csrfToken,
        message: this.state.testMessage,
        onSuccess: (json) => {
          this.setState({
            highlightedTriggerText: json.activatedTrigger,
            inputValues: json.paramValues,
            isTestingTriggers: false,
            hasTestedTriggers: true,
            triggerErrorOccurred: false
          });
        },
        onError: () => {
          this.setState({
            highlightedTriggerText: null,
            inputValues: {},
            isTestingTriggers: false,
            hasTestedTriggers: false,
            triggerErrorOccurred: true
          });
        }
      });
    }

    fetchResult(): void {
      this.setState({
        isTestingResult: true,
        resultErrorOccurred: false
      });
      testInvocation({
        behaviorId: this.props.behaviorId,
        paramValues: this.state.inputValues,
        csrfToken: this.props.csrfToken,
        onSuccess: this.didTestInvocation,
        onError: () => {
          this.setState({
            resultErrorOccurred: true,
            isTestingResult: false
          });
        }
      });
    }

    didTestInvocation(json: BehaviorInvocationTestReportOutput): void {
      const newResults = this.state.results.concat(InvocationTestResult.fromReportJSON(json));
      this.setState({
        results: newResults,
        isTestingResult: false,
        hasTestedResult: true
      });
    }

    focus(): void {
      if (this.testMessage) {
        this.testMessage.focus();
      }
    }

    getTriggers(): Array<Trigger> {
      return this.props.triggers.filter((trigger) => !!trigger.text);
    }

    getResults(): Array<InvocationTestResult>  {
      return this.state.results;
    }

    hasResult(): boolean {
      return this.getResults().length > 0;
    }

    getValueForInputName(name: string) {
      return (
        <FormInput
          className="form-input-borderless"
          value={this.state.inputValues[name] || ''}
          onChange={this.onChangeInputValue.bind(this, name)}
          placeholder="None"
        />
      );
    }

    getTriggerTestingStatus() {
      if (this.state.isTestingTriggers) {
        return (
          <span className="type-weak type-italic pulse">— testing “{this.state.testMessage}”…</span>
        );
      } else if (this.state.highlightedTriggerText) {
        return (
          <span className="type-green">— successful match</span>
        );
      } else if (this.state.triggerErrorOccurred) {
        return (
          <span className="type-pink">— an error occurred; try again</span>
        );
      } else if (this.state.testMessage && this.state.hasTestedTriggers) {
        return (
          <span className="type-pink">— no match for “{this.state.testMessage}”</span>
        );
      } else {
        return null;
      }
    }

    countNonEmptyInputsProvided(): number {
      return Object.keys(this.state.inputValues).filter((inputName) => {
        return this.state.inputValues[inputName] != null;
      }).length;
    }

    getInputTestingStatus() {
      var numInputValues = this.countNonEmptyInputsProvided();
      if (this.state.isTestingTriggers || numInputValues === 0 || this.props.inputs.length === 0) {
        return "";
      } else if (numInputValues === 1) {
        return (
          <span className="type-green">— 1 value collected</span>
        );
      } else if (numInputValues > 1) {
        return (
          <span className="type-green">— {numInputValues} values collected</span>
        );
      } else {
        return null;
      }
    }

    render() {
      return (
        <div>
          <Collapsible revealWhen={this.hasResult()}>
            <InvocationResults
              results={this.getResults()}
            />
          </Collapsible>
          <div className="box-action phn">
            <div className="container">
              <div className="columns">
                <div className="column column-page-sidebar">
                  <h4 className="mtn type-weak">Test the skill</h4>
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
      var apps = this.props.appsRequiringAuth;
      if (this.props.behaviorId && apps.length > 0) {
        return (
          <div>
            <TesterAuthRequired
              groupId={this.props.groupId}
              behaviorId={this.props.behaviorId}
              appsRequiringAuth={this.props.appsRequiringAuth}
            />

            <div className="mtxl">
              <button className="mrs" type="button" onClick={this.onDone}>Cancel</button>
            </div>
          </div>
        );
      } else {
        return this.renderTester();
      }
    }

    renderTester() {
      const triggers = this.getTriggers();
      const inputs = this.props.inputs;
      return (
        <div>

          {triggers.length > 0 ? this.renderTriggers(triggers) : this.renderNoTriggers()}

          <h4 className="mbxs">
            <span>User input </span>
            <span>{this.getInputTestingStatus()}</span>
          </h4>
          {inputs.length > 0 ? this.renderInputs(inputs) : this.renderNoInputs()}

          <div className="columns columns-elastic mvxl">
            <div className="column column-expand">
              <DynamicLabelButton
                className="mrs button-primary"
                onClick={this.fetchResult}
                disabledWhen={this.state.isTestingResult}
                labels={[{
                  text: 'Test response',
                  displayWhen: !this.state.isTestingResult
                }, {
                  text: 'Testing',
                  displayWhen: this.state.isTestingResult
                }]}
              />
              <span className="align-button">{this.renderResultStatus()}</span>
            </div>

            <div className="column column-shrink">
              <button className="mrs" type="button" onClick={this.onDone}>Done</button>
            </div>
          </div>
        </div>
      );
    }

    renderTriggers(triggers: Array<Trigger>) {
      return (
        <div>
          <p>
            Type a message to see whether it matches any of the triggers and, if so, what
            user input is collected.
          </p>

          <div className="mbxl">
            <FormInput ref={(el) => this.testMessage = el}
              value={this.state.testMessage}
              onChange={this.onChangeTestMessage}
              placeholder="Enter message"
            />
          </div>

          <h4 className="mbxs">
            <span>Triggers </span>
            <span>{this.getTriggerTestingStatus()}</span>
          </h4>
          <div className="mbxl type-s">
            {triggers.map(this.renderTrigger)}
          </div>
        </div>
      );
    }

    renderNoTriggers() {
      return (
        <div className="mbxl">
          <h4 className="mbxs">Triggers</h4>
          <p className="type-weak">No triggers have been defined.</p>
        </div>
      );
    }

    renderTrigger(trigger: Trigger, index: number) {
      var highlighted = this.state.highlightedTriggerText === trigger.text;
      var className = "pvxs " +
        (trigger.isRegex ? " type-monospace " : "") +
        (highlighted ? " type-semibold type-green " : "");
      return (
        <div ref={`trigger${index}`} key={`trigger${index}`} className={className}>
          {trigger.text} {highlighted ? "✓" : ""}
        </div>
      );
    }

    renderInputs(inputs: Array<Input>) {
      return (
        <div className="columns columns-elastic">
          <div className="column-group">
            {inputs.map((input, index) => (
              <div key={`input${index}`} className="column-row">
                <div className="column column-shrink type-monospace type-weak type-s prs pts">{input.name}:</div>
                <div className="column column-expand">{this.getValueForInputName(input.name)}</div>
              </div>
            ))}
          </div>
        </div>
      );
    }

    renderNoInputs() {
      return (
        <p className="type-weak">No user input has been defined.</p>
      );
    }

    renderResultStatus() {
      if (this.state.resultErrorOccurred) {
        return (
          <span className="type-pink">— An error occurred while testing <b>{this.state.testMessage}</b></span>
        );
      } else {
        return (
          <span>
            {this.renderResultStatusTriggerText()}
            {this.renderResultStatusInputText()}
          </span>
        );
      }
    }

    renderResultStatusTriggerText() {
      if (this.state.highlightedTriggerText && this.state.testMessage) {
        return (
          <span>— Use <b>{this.state.testMessage}</b> </span>
        );
      } else if (this.getTriggers().length) {
        return (
          <span>— Simulate any trigger </span>
        );
      } else {
        return null;
      }
    }

    renderResultStatusInputText() {
      var numInputs = this.props.inputs.length;
      var numInputValues = this.countNonEmptyInputsProvided();
      if (numInputs === 0) {
        return null;
      } else if (numInputValues === 0) {
        return (
          <span className="type-weak">(with no user input collected)</span>
        );
      } else if (numInputValues === 1) {
        return (
          <span className="type-weak">(with 1 user input collected)</span>
        );
      } else {
        return (
          <span className="type-weak">(with {numInputValues} user inputs collected)</span>
        );
      }
    }
}

export default BehaviorTester;
