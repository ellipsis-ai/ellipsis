define(function(require) {
  var React = require('react'),
    BehaviorTest = require('./behavior_test'),
    Collapsible = require('../shared_ui/collapsible'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    ifPresent = require('../lib/if_present'),
    FormInput = require('../form/input'),
    Input = require('../models/input'),
    Trigger = require('../models/trigger'),
    debounce = require('javascript-debounce'),
    oauth2ApplicationShape = require('./oauth2_application_shape'),
    TesterAuthRequired = require('./tester_auth_required'),
    InvocationTestResult = require('../models/behavior_invocation_result'),
    InvocationResults = require('./behavior_tester_invocation_results');

  return React.createClass({
    displayName: 'BehaviorTester',
    propTypes: {
      triggers: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Trigger)).isRequired,
      inputs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Input)).isRequired,
      behaviorId: React.PropTypes.string,
      csrfToken: React.PropTypes.string.isRequired,
      onDone: React.PropTypes.func.isRequired,
      appsRequiringAuth: React.PropTypes.arrayOf(oauth2ApplicationShape).isRequired
    },

    getInitialState: function() {
      return {
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
    },

    componentWillReceiveProps: function(newProps) {
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
    },

    onChangeTestMessage: function(value) {
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
    },

    onChangeInputValue: function(name, value) {
      var newInputValues = Object.assign({}, this.state.inputValues);
      newInputValues[name] = value;
      this.setState({
        inputValues: newInputValues
      });
    },

    onDone: function() {
      this.props.onDone();
    },

    validateMessage: debounce(function() {
      this.setState({
        isTestingTriggers: true
      }, this.sendValidationRequest);
    }, 500),

    sendValidationRequest: function() {
      BehaviorTest.testTriggers({
        behaviorId: this.props.behaviorId,
        csrfToken: this.props.csrfToken,
        message: this.state.testMessage,
        onSuccess: (json) => {
          this.setState({
            highlightedTriggerText: json.activatedTrigger,
            inputValues: json.inputValues,
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
    },

    fetchResult: function() {
      this.setState({
        isTestingResult: true,
        resultErrorOccurred: false
      });
      BehaviorTest.testInvocation({
        behaviorId: this.props.behaviorId,
        inputValues: this.state.inputValues,
        csrfToken: this.props.csrfToken,
        onSuccess: (json) => {
          var newResults = this.state.results.concat(new InvocationTestResult(
            json.result && json.result.fullText,
            json.missingInputNames,
            json.missingSimpleTokens,
            json.missingUserEnvVars
          ));
          this.setState({
            results: newResults,
            isTestingResult: false,
            hasTestedResult: true
          });
        },
        onError: () => {
          this.setState({
            resultErrorOccurred: true,
            isTestingResult: false
          });
        }
      });
    },

    focus: function() {
      if (this.refs.testMessage) {
        this.refs.testMessage.focus();
      }
    },

    getTriggers: function() {
      return this.props.triggers.filter((trigger) => !!trigger.text);
    },

    getResults: function() {
      return this.state.results;
    },

    hasResult: function() {
      return this.getResults().length > 0;
    },

    getValueForInputName: function(name) {
      return (
        <FormInput
          className="form-input-borderless"
          value={this.state.inputValues[name] || ''}
          onChange={this.onChangeInputValue.bind(this, name)}
          placeholder="None"
        />
      );
    },

    getTriggerTestingStatus: function() {
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
      }
    },

    countNonEmptyInputsProvided: function() {
      return Object.keys(this.state.inputValues).filter((inputName) => {
        return this.state.inputValues[inputName] != null;
      }).length;
    },

    getInputTestingStatus: function() {
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
      }
    },

    render: function() {
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
                  <h4 className="type-weak">Test the skill</h4>
                </div>
                <div className="column column-three-quarters pll mobile-pln mobile-column-full">
                  {this.renderContent()}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderContent: function() {
      var apps = this.props.appsRequiringAuth;
      if (apps.length > 0) {
        return (
          <TesterAuthRequired behaviorId={this.props.behaviorId} appsRequiringAuth={apps}/>
        );
      } else {
        return ifPresent(this.getTriggers(), this.renderTriggerTester, this.renderNoTriggers);
      }
    },

    renderTriggerTester: function(triggers) {
      return (
        <div>

          <p>
            Type a message to see whether it matches any of the triggers and, if so, what
            user input is collected.
          </p>

          <div className="mbxl">
            <FormInput ref="testMessage"
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

          <h4 className="mbxs">
            <span>User input </span>
            <span>{this.getInputTestingStatus()}</span>
          </h4>
          {ifPresent(this.props.inputs, this.renderInputs, this.renderNoInputs)}

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
    },

    renderTrigger: function(trigger, index) {
      var highlighted = this.state.highlightedTriggerText === trigger.text;
      var className = "pvxs " +
        (trigger.isRegex ? " type-monospace " : "") +
        (highlighted ? " type-bold type-green " : "");
      return (
        <div ref={`trigger${index}`} key={`trigger${index}`} className={className}>
          {trigger.text} {highlighted ? "✓" : ""}
        </div>
      );
    },

    renderNoTriggers: function() {
      return (
        <div>
          <p>This skill does not have any triggers. Add at least one trigger before testing.</p>

          <div className="mvxl">
            <button type="button" onClick={this.props.onDone}>OK</button>
          </div>
        </div>
      );
    },

    renderInputs: function(inputs) {
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
    },

    renderNoInputs: function() {
      return (
        <p>No user input has been defined.</p>
      );
    },

    renderResultStatus: function() {
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
    },

    renderResultStatusTriggerText: function() {
      if (this.state.highlightedTriggerText && this.state.testMessage) {
        return (
          <span>— Use <b>{this.state.testMessage}</b> </span>
        );
      } else {
        return (
          <span>— Simulate any trigger </span>
        );
      }
    },

    renderResultStatusInputText: function() {
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
  });
});
