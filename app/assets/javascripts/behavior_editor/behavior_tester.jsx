define(function(require) {
  var React = require('react'),
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
      behaviorId: React.PropTypes.string.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      onDone: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        testMessage: '',
        highlightedTriggerText: null,
        paramValues: {},
        isTesting: false,
        hasTested: false,
        errorOccurred: false
      };
    },

    componentWillReceiveProps: function(newProps) {
      if (this.props.triggers !== newProps.triggers) {
        this.setState(this.getInitialState());
      }
    },

    onChangeTestMessage: function(value) {
      this.setState({
        testMessage: value,
        highlightedTriggerText: null,
        paramValues: {},
        hasTested: false,
        errorOccurred: false
      });
      if (value) {
        this.validateMessage();
      }
    },

    validateMessage: debounce(function() {
      this.setState({
        isTesting: true
      }, this.sendValidationRequest);
    }, 500),

    sendValidationRequest: function() {
      var formData = new FormData();
      formData.append('message', this.state.testMessage);
      formData.append('behaviorId', this.props.behaviorId);
      fetch(jsRoutes.controllers.BehaviorEditorController.test().url, {
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
          console.log(json);
          this.setState({
            highlightedTriggerText: json.activatedTrigger || null,
            paramValues: json.paramValues,
            isTesting: false,
            hasTested: true,
            errorOccurred: false
          });
        })
        .catch(() => {
          this.setState({
            highlightedTriggerText: null,
            paramValues: {},
            isTesting: false,
            hasTested: false,
            errorOccurred: true
          });
        });
    },

    focus: function() {
      this.refs.testMessage.focus();
    },

    getTriggers: function() {
      return this.props.triggers.filter((trigger) => !!trigger.text);
    },

    getValueForParamName: function(name) {
      return ifPresent(this.state.paramValues[name], (value) => (
        <span>{value}</span>
      ), () => (
        <span className="type-disabled">None</span>
      ));
    },

    getTriggerTestingStatus: function() {
      if (this.state.isTesting) {
        return (
          <span className="type-weak type-italic pulse">— testing “{this.state.testMessage}”…</span>
        );
      } else if (this.state.highlightedTriggerText) {
        return (
          <span className="type-green">— successful match</span>
        );
      } else if (this.state.errorOccurred) {
        return (
          <span className="type-pink">— an error occurred; try again</span>
        );
      } else if (this.state.testMessage && this.state.hasTested) {
        return (
          <span className="type-pink">— no match for “{this.state.testMessage}”</span>
        );
      }
    },

    getParamTestingStatus: function() {
      var numParamValues = Object.keys(this.state.paramValues).length;
      if (this.state.isTesting || numParamValues === 0 || this.props.params.length === 0) {
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
        <div className="box-action">
          <div className="container phn">
            <div className="columns">
              <div className="column column-one-quarter mobile-column-full">
                <h4 className="type-weak">Test the behavior</h4>
              </div>
              <div className="column column-three-quarters pll mobile-pln mobile-column-full">
                {ifPresent(this.getTriggers(), this.renderTriggerTester, this.renderNoTriggers)}

                <div className="mvxl">
                  <button type="button" onClick={this.props.onDone}>Done</button>
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
            Type a message to test whether it matches any of the triggers and, if so, what
            user input is collected.
          </p>

          <div className="mbxl">
            <Input ref="testMessage" value={this.state.testMessage} onChange={this.onChangeTestMessage}/>
          </div>

          <h4 className="mbs">
            <span>Triggers </span>
            <span>{this.getTriggerTestingStatus()}</span>
          </h4>
          <div className="border-top mbxl">
            {triggers.map(this.renderTrigger)}
          </div>

          <h4 className="mbxs">
            <span>User input </span>
            <span>{this.getParamTestingStatus()}</span>
          </h4>
          {ifPresent(this.props.params, this.renderParams, this.renderNoParams)}
        </div>
      );
    },

    renderTrigger: function(trigger, index) {
      var highlighted = this.state.highlightedTriggerText === trigger.text;
      var className = "pvs border-bottom " +
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
    }
  });
});
