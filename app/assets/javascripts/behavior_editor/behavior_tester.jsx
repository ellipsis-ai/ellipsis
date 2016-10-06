define(function(require) {
  var React = require('react'),
    ifPresent = require('../if_present'),
    Input = require('../form/input'),
    Trigger = require('../models/trigger'),
    debounce = require('javascript-debounce');
  require('whatwg-fetch');

  return React.createClass({
    displayName: 'BehaviorTester',
    propTypes: {
      triggers: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Trigger)).isRequired,
      behaviorId: React.PropTypes.string.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      onDone: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        testMessage: '',
        highlightedIndex: null
      };
    },

    componentWillReceiveProps: function(newProps) {
      if (this.props.triggers !== newProps.triggers) {
        this.setState(this.getInitialState());
      }
    },

    onChangeTestMessage: function(value) {
      this.setState({ testMessage: value });
      this.clearHighlightedTrigger();
      if (value) {
        this.validateMessage();
      }
    },

    validateMessage: debounce(function() {
      this.sendValidationRequest();
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
          if (json.activatedTrigger === "\<no match\>") {
            this.clearHighlightedTrigger();
          } else {
            this.highlightTriggerWithText(json.activatedTrigger);
          }
        })
        .catch((error) => {
          console.log(error);
        });
    },

    highlightTriggerWithText: function(triggerText) {
      var index = this.props.triggers.findIndex((trigger) => trigger.text === triggerText);
      if (index >= 0) {
        this.setState({ highlightedIndex: index });
      } else {
        this.clearHighlightedTrigger();
      }
    },

    clearHighlightedTrigger: function() {
      this.setState({ highlightedIndex: null });
    },

    render: function() {
      return (
        <div className="box-action">
          <div className="container phn">
            {ifPresent(this.props.triggers, this.renderTriggerTester, this.renderNoTriggers)}

            <div className="mvxl">
              <button type="button" onClick={this.props.onDone}>Done</button>
            </div>
          </div>
        </div>
      );
    },

    renderTriggerTester: function(triggers) {
      return (
        <div>

          <p>
            Type a message to test if it matches any of the triggers for your behavior.
          </p>

          <div className="mbxl">
            <Input autoFocus={true} value={this.state.testMessage} onChange={this.onChangeTestMessage} />
          </div>

          <h5>Triggers</h5>
          <div className="border-top">
            {triggers.map(this.renderTrigger)}
          </div>
        </div>
      );
    },

    renderTrigger: function(trigger, index) {
      var highlighted = this.state.highlightedIndex === index;
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
          <p>This behavior does not have any triggers. Add one or more triggers before testing.</p>
        </div>
      );
    }
  });
});
