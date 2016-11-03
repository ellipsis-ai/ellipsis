define(function(require) {
  var React = require('react'),
    BehaviorTest = require('./behavior_test'),
    Collapsible = require('../collapsible'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    ifPresent = require('../if_present'),
    Input = require('../form/input'),
    Param = require('../models/param'),
    Trigger = require('../models/trigger'),
    debounce = require('javascript-debounce'),
    oauth2ApplicationShape = require('./oauth2_application_shape'),
    TesterAuthRequired = require('./tester_auth_required');

  class TestResult {
    constructor(response, missingParamNames) {
      this.response = response || '';
      this.missingParamNames = missingParamNames || [];
    }
  }

  return React.createClass({
    displayName: 'BehaviorTester',
    propTypes: {
      triggers: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Trigger)).isRequired,
      params: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Param)).isRequired,
      behaviorId: React.PropTypes.string,
      csrfToken: React.PropTypes.string.isRequired,
      onDone: React.PropTypes.func.isRequired,
      appsRequiringAuth: React.PropTypes.arrayOf(oauth2ApplicationShape).isRequired
    },

    getInitialState: function() {
      return {
        testMessage: '',
        highlightedTriggerText: null,
        paramValues: {},
        isTestingTriggers: false,
        isTestingResult: false,
        hasTestedTriggers: false,
        hasTestedResult: false,
        triggerErrorOccurred: false,
        resultErrorOccurred: false,
        resultMissingParamNames: [],
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
      if (JSON.stringify(this.props.params) !== JSON.stringify(newProps.params)) {
        this.setState({
          paramValues: {}
        });
      }
    },

    onChangeTestMessage: function(value) {
      this.setState({
        testMessage: value,
        highlightedTriggerText: null,
        paramValues: {},
        hasTestedTriggers: false,
        triggerErrorOccurred: false,
        resultErrorOccurred: false
      });
      if (value) {
        this.validateMessage();
      }
    },

    onChangeParamValue: function(name, value) {
      var newParamValues = Object.assign({}, this.state.paramValues);
      newParamValues[name] = value;
      this.setState({
        paramValues: newParamValues
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
            paramValues: json.paramValues,
            isTestingTriggers: false,
            hasTestedTriggers: true,
            triggerErrorOccurred: false
          });
        },
        onError: () => {
          this.setState({
            highlightedTriggerText: null,
            paramValues: {},
            isTestingTriggers: false,
            hasTestedTriggers: false,
            triggerErrorOccurred: true
          });
        }
      });
    },

    missingParametersResult: function(missingParamNames) {
      if (missingParamNames.length === 1) {
        return (
          <div>Ellipsis will ask the user for a value for <code className="type-bold mlxs">{missingParamNames[0]}</code>.</div>
        );
      } else {
        return (
          <div>
            <span>Ellipsis will ask the user for values for these inputs: </span>
            <code className="type-bold mlxs">{missingParamNames.join(", ")}</code>
          </div>
        );
      }
    },

    fetchResult: function() {
      this.setState({
        isTestingResult: true,
        resultErrorOccurred: false
      });
      BehaviorTest.testInvocation({
        behaviorId: this.props.behaviorId,
        paramValues: this.state.paramValues,
        csrfToken: this.props.csrfToken,
        onSuccess: (json) => {
          var newResults = this.state.results.concat(new TestResult(
            json.result && json.result.fullText,
            json.missingParamNames
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

    getValueForParamName: function(name) {
      return (
        <Input
          className="form-input-borderless"
          value={this.state.paramValues[name] || ''}
          onChange={this.onChangeParamValue.bind(this, name)}
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

    countNonEmptyParamsProvided: function() {
      return Object.keys(this.state.paramValues).filter((paramName) => {
        return this.state.paramValues[paramName] != null;
      }).length;
    },

    getParamTestingStatus: function() {
      var numParamValues = this.countNonEmptyParamsProvided();
      if (this.state.isTestingTriggers || numParamValues === 0 || this.props.params.length === 0) {
        return "";
      } else if (numParamValues === 1) {
        return (
          <span className="type-green">— 1 value collected</span>
        );
      } else if (numParamValues > 1) {
        return (
          <span className="type-green">— {numParamValues} values collected</span>
        );
      }
    },

    componentDidUpdate: function() {
      this.refs.results.scrollTop = this.refs.results.scrollHeight - this.refs.results.clientHeight;
    },

    render: function() {
      return (
        <div>
          <Collapsible revealWhen={this.hasResult()}>
            <div className="box-help">
              <div className="container phn">
                <div className="columns">
                  <div className="column column-one-quarter mobile-column-full"></div>
                  <div className="column column-three-quarters pll mobile-pln mobile-column-full">

                    <h4>Response log</h4>
                    <div ref="results" className="type-s" style={{
                      maxHeight: "12rem",
                      overflow: "auto"
                    }}>
                      {this.getResults().map((result, index) => {
                        return (
                          <div
                            className={
                              "mbxs " +
                              (index + 1 === this.getResults().length ? "type-black" : "type-disabled")
                            }
                          >
                            {ifPresent(result.response, (response) => (
                              <div className="display-overflow-scroll border border-blue pas bg-blue-lightest">
                                <pre>{response}</pre>
                              </div>
                            ))}
                            {ifPresent(result.missingParamNames, this.missingParametersResult)}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </Collapsible>
          <div className="box-action">
            <div className="container phn">
              <div className="columns">
                <div className="column column-one-quarter mobile-column-full">
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
            <Input ref="testMessage"
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
            <span>{this.getParamTestingStatus()}</span>
          </h4>
          {ifPresent(this.props.params, this.renderParams, this.renderNoParams)}

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

    renderParams: function(params) {
      return (
        <div className="columns columns-elastic">
          <div className="column-group">
            {params.map((param, index) => (
              <div key={`param${index}`} className="column-row">
                <div className="column column-shrink type-monospace type-weak type-s prs pts">{param.name}:</div>
                <div className="column column-expand">{this.getValueForParamName(param.name)}</div>
              </div>
            ))}
          </div>
        </div>
      );
    },

    renderNoParams: function() {
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
            {this.renderResultStatusParamText()}
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

    renderResultStatusParamText: function() {
      var numParams = this.props.params.length;
      var numParamValues = this.countNonEmptyParamsProvided();
      if (numParams === 0) {
        return null;
      } else if (numParamValues === 0) {
        return (
          <span className="type-weak">(with no user input collected)</span>
        );
      } else if (numParamValues === 1) {
        return (
          <span className="type-weak">(with 1 user input collected)</span>
        );
      } else {
        return (
          <span className="type-weak">(with {numParamValues} user inputs collected)</span>
        );
      }
    }
  });
});
