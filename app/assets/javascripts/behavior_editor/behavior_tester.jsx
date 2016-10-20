define(function(require) {
  var React = require('react'),
    Collapsible = require('../collapsible'),
    ifPresent = require('../if_present'),
    Input = require('../form/input'),
    Param = require('../models/param'),
    Trigger = require('../models/trigger'),
    debounce = require('javascript-debounce');
  require('whatwg-fetch');

  return React.createClass({
    displayName: 'BehaviorTester',
    propTypes: {
      triggers: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Trigger)).isRequired,
      params: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Param)).isRequired,
      behaviorId: React.PropTypes.string,
      csrfToken: React.PropTypes.string.isRequired,
      onDone: React.PropTypes.func.isRequired
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
        result: ''
      };
    },

    componentWillReceiveProps: function(newProps) {
      if (this.props.triggers !== newProps.triggers) {
        this.setState(this.getInitialState());
      }
    },

    isSavedBehavior: function() {
      return !!this.props.behaviorId;
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
      if (value && this.isSavedBehavior()) {
        this.validateMessage();
      }
    },

    onDone: function() {
      this.props.onDone();
      this.setState(this.getInitialState());
    },

    validateMessage: debounce(function() {
      this.setState({
        isTestingTriggers: true
      }, this.sendValidationRequest);
    }, 500),

    sendValidationRequest: function() {
      var formData = new FormData();
      formData.append('message', this.state.testMessage);
      formData.append('behaviorId', this.props.behaviorId);
      fetch(jsRoutes.controllers.BehaviorEditorController.testTriggers().url, {
        credentials: 'same-origin',
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Csrf-Token': this.props.csrfToken
        },
        body: formData
      })
        .then((response) => response.json())
        .then((json) => {
          this.setState({
            highlightedTriggerText: json.activatedTrigger,
            paramValues: json.paramValues,
            isTestingTriggers: false,
            hasTestedTriggers: true,
            triggerErrorOccurred: false
          });
        })
        .catch(() => {
          this.setState({
            highlightedTriggerText: null,
            paramValues: {},
            isTestingTriggers: false,
            hasTestedTriggers: false,
            triggerErrorOccurred: true
          });
        });
    },

    fetchResult: function() {
      this.setState({
        isTestingResult: true,
        result: '',
        resultErrorOccurred: false
      });
      var formData = new FormData();
      var jsonParams = JSON.stringify(
        Object.keys(this.state.paramValues).map((k) => this.state.paramValues[k])
      );
      formData.append('behaviorId', this.props.behaviorId);
      formData.append('paramValuesJson', jsonParams);
      fetch(jsRoutes.controllers.BehaviorEditorController.testInvocation().url, {
        credentials: 'same-origin',
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Csrf-Token': this.props.csrfToken
        },
        body: formData
      })
        .then((response) => response.json())
        .then((json) => {
          this.setState({
            result: json.fullText,
            isTestingResult: false,
            hasTestedResult: true
          });
        })
        .catch(() => {
          this.setState({
            resultErrorOccurred: true,
            isTestingResult: false
          });
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

    getResult: function() {
      return this.state.result;
    },

    getValueForParamName: function(name) {
      return ifPresent(this.state.paramValues[name], (value) => (
        <span>{value}</span>
      ), () => (
        <span className="type-disabled">None</span>
      ));
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

    getParamTestingStatus: function() {
      var nonEmptyParams = Object.keys(this.state.paramValues).filter((paramName) => {
        return this.state.paramValues[paramName] != null;
      });
      var numParamValues = nonEmptyParams.length;
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

    render: function() {
      return (
        <div>
          <Collapsible revealWhen={!!(this.getResult() && !this.state.isTestingResult)}>
            <div className="box-help">
              <div className="container phn">
                <div className="columns">
                  <div className="column column-one-quarter mobile-column-full"></div>
                  <div className="column column-three-quarters pll mobile-pln mobile-column-full">

                    <h4>Response</h4>
                    <div className="display-overflow-scroll border border-blue pas bg-blue-lightest"
                      style={{
                        maxHeight: "10.25em",
                        overflow: "auto"
                      }}
                    >
                      <pre>{this.state.result}</pre>
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
                  <h4 className="type-weak">Test the behavior</h4>
                </div>
                <div className="column column-three-quarters pll mobile-pln mobile-column-full">
                  {ifPresent(this.getTriggers(), this.renderTriggerTester, this.renderNoTriggers)}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderTriggerTester: function(triggers) {
      return (
        <div>

          <p>
            Type a message to see whether it matches any of the triggers and, if so, what
            user input is collected.
          </p>

          <div className="mbxl">
            <Input ref="testMessage" value={this.state.testMessage} onChange={this.onChangeTestMessage}/>
          </div>

          <h4 className="mbxs">
            <span>Triggers </span>
            <span>{this.getTriggerTestingStatus()}</span>
          </h4>
          <div className="border-top mbxl type-s">
            {triggers.map(this.renderTrigger)}
          </div>

          <h4 className="mbxs">
            <span>User input </span>
            <span>{this.getParamTestingStatus()}</span>
          </h4>
          {ifPresent(this.props.params, this.renderParams, this.renderNoParams)}

          <div className="mvxl">
            <button className="mrs" type="button" onClick={this.onDone}>Done</button>
            <button className="mrs" type="button"
              onClick={this.fetchResult}
              disabled={!this.state.highlightedTriggerText || this.state.isTestingResult}
            >Test response</button>
            <span className="align-button">{this.renderResultStatus()}</span>
          </div>
        </div>
      );
    },

    renderTrigger: function(trigger, index) {
      var highlighted = this.state.highlightedTriggerText === trigger.text;
      var className = "pvxs border-bottom " +
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
          <p>This behavior does not have any triggers. Add at least one trigger before testing.</p>

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
                <div className="column column-shrink type-monospace type-weak prs pvxs">{param.name}:</div>
                <div className="column column-expand pvxs">{this.getValueForParamName(param.name)}</div>
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
      if (this.state.isTestingResult) {
        return (
          <span className="type-weak pulse">— Testing <b>{this.state.testMessage}</b></span>
        );
      } else if (this.state.resultErrorOccurred) {
        return (
          <span className="type-pink">— An error occurred testing <b>{this.state.testMessage}</b></span>
        );
      } else if (this.state.highlightedTriggerText) {
        return (
          <span>— Use <b>{this.state.testMessage}</b></span>
        );
      } else {
        return (
          <span>— Requires a matched trigger</span>
        );
      }
    }
  });
});
