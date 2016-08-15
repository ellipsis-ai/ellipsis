define(function(require) {
var React = require('react'),
  debounce = require('javascript-debounce'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  Checkbox = require('./checkbox'),
  DeleteButton = require('./delete_button'),
  HelpButton = require('./help_button'),
  Input = require('./input'),
  Collapsible = require('../collapsible');
  require('es6-promise');
  require('whatwg-fetch');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    caseSensitive: React.PropTypes.bool.isRequired,
    className: React.PropTypes.string,
    helpVisible: React.PropTypes.bool.isRequired,
    hideDelete: React.PropTypes.bool.isRequired,
    id: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.string
    ]).isRequired,
    includeHelp: React.PropTypes.bool.isRequired,
    isRegex: React.PropTypes.bool.isRequired,
    onChange: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEnterKey: React.PropTypes.func.isRequired,
    onHelpClick: React.PropTypes.func.isRequired,
    requiresMention: React.PropTypes.bool.isRequired,
    value: React.PropTypes.string.isRequired
  },
  getInitialState: function() {
    return {
      highlightCaseSensitivity: false,
      regexError: null,
      showError: false
    };
  },
  clearError: function() {
    this.setState({
      regexError: null,
      showError: false
    });
  },
  changeTrigger: function(props) {
    var newTrigger = {
      text: this.props.value,
      requiresMention: this.props.requiresMention,
      isRegex: this.props.isRegex,
      caseSensitive: this.props.caseSensitive
    };
    Object.keys(props).forEach(function(key) {
      newTrigger[key] = props[key];
    });
    this.props.onChange(newTrigger);
    if (newTrigger.isRegex) {
      this.validateTrigger();
    } else {
      this.clearError();
    }
  },
  onChange: function(propName, newValue) {
    var changes = {};
    changes[propName] = newValue;
    this.changeTrigger(changes);
    this.focus();
  },
  onBlur: function(newValue) {
    var text = newValue;
    var changes = {};
    if (this.props.isRegex && this.props.caseSensitive && text.indexOf("(?i)") === 0) {
      text = text.replace(/^\(\?i\)/, '');
      changes.caseSensitive = false;
      changes.text = text;
      this.changeTrigger(changes);
      this.setState({ highlightCaseSensitivity: true });
      var callback = function() {
        this.setState({ highlightCaseSensitivity: false });
      }.bind(this);
      window.setTimeout(callback, 1000);
    }
  },
  validateTrigger: debounce(function() {
    if (!this.props.value) {
      this.clearError();
      return;
    }

    var url = jsRoutes.controllers.ApplicationController.regexValidationErrorsFor(this.props.value).url;
    fetch(url, { credentials: 'same-origin' })
      .then(function(response) {
        return response.json();
      }).then(function(json) {
        var error = json[0].length ? json[0][0] : null;
        this.setState({
          validated: true,
          regexError: error,
          showError: !!(this.state.showError && error)
        });
      }.bind(this)).catch(function() {
        // TODO: figure out what to do if there's a request error; for now clear user-visible errors
        this.clearError();
      });
  }, 500),
  isEmpty: function() {
    return !this.props.value;
  },
  toggleError: function() {
    this.setState({ showError: !this.state.showError });
    this.focus();
  },
  toggleHelp: function() {
    this.props.onHelpClick();
  },
  focus: function() {
    this.refs.input.focus();
  },

  componentDidMount: function() {
    if (this.props.isRegex) {
      this.validateTrigger();
    }
  },

  render: function() {
    return (
      <div className="columns columns-elastic mobile-columns-float mbs mobile-mbxl">
        <div className="column column-expand prn">
          <div className="columns columns-elastic">
            <div className={"column column-shrink prn " + (this.props.requiresMention ? "" : "display-none")}>
              <div className={
                "type-weak type-s form-input form-input-borderless prxs " +
                (this.props.className || "")
              }>
                <label htmlFor={this.props.id}>@ellipsis:</label>
              </div>
            </div>
            <div className={"column column-shrink prn " + (this.props.isRegex ? "" : "display-none")}>
              <div className={"type-disabled type-monospace form-input form-input-borderless " + (this.props.className || "")}>
                <label htmlFor={this.props.id}>/</label>
              </div>
            </div>
            <div className="column column-expand prn position-relative">
              <Input
                className={
                  " form-input-borderless " +
                  (this.props.isRegex ? " type-monospace " : "") +
                  (this.props.className || "")
                }
                id={this.props.id}
                ref="input"
                value={this.props.value}
                placeholder="Add a trigger phrase"
                onChange={this.onChange.bind(this, 'text')}
                onBlur={this.onBlur}
                onEnterKey={this.props.onEnterKey}
              />
              {this.state.regexError ? (
                <div className="position-absolute position-top-right mts mrxs fade-in">
                  <button type="button"
                    className="button-error button-s button-shrink"
                    ref="errorButton"
                    onClick={this.toggleError}
                  >
                    <span>{this.state.showError ? "▾" : "▸" }</span>
                    <span> Error</span>
                  </button>
                </div>
              ) : ""}
              <Collapsible revealWhen={this.state.showError} className="popup display-limit-width">
                <div className="border bg-blue-lighter border-blue border-error-top pts phm type-s popup-shadow">
                  <div className="position-absolute position-top-right ptxs prxs">
                    <HelpButton onClick={this.toggleError} toggled={true} inline={true} />
                  </div>
                  <div><b>This regex trigger cannot be used because of a format error:</b></div>
                  <pre>{this.state.regexError || "\n\n\n"}</pre>
                </div>
              </Collapsible>
            </div>
            <div className={"column column-shrink prn " + (this.props.isRegex ? "" : "display-none")}>
              <div className={"type-disabled type-monospace form-input form-input-borderless prs " + (this.props.className || "")}>
                <label htmlFor={this.props.id}>/</label>
              </div>
            </div>
          </div>
        </div>
        <div className="column column-shrink prn position-relative">
          <div className={"display-ellipsis form-input form-input-borderless " +
            (this.props.className || "")}>
            {this.props.includeHelp ? (
              <HelpButton onClick={this.toggleHelp} toggled={this.props.helpVisible} className="align-m mrs" />
              ) : ""}
            <label className="mrm type-s" title="Only respond when someone mentions @ellipsis">
              <Checkbox
                checked={this.props.requiresMention}
                onChange={this.onChange.bind(this, 'requiresMention')}
              /> <span>🗣 🤖</span>
            </label>
            <label
              className={"mrm type-s " + (this.state.highlightCaseSensitivity ? "blink-twice" : "")}
              title="Match uppercase and lowercase letters exactly — if unchecked, case is ignored"
            >
              <Checkbox
                checked={this.props.caseSensitive}
                onChange={this.onChange.bind(this, 'caseSensitive')}
              /> <i>Aa</i>
            </label>
            <label className="type-s" title="Use regular expression pattern matching">
              <Checkbox
                checked={this.props.isRegex}
                onChange={this.onChange.bind(this, 'isRegex')}
              /> <code>/^…$/</code>
            </label>
          </div>
        </div>
        <div className="column column-shrink">
          <DeleteButton onClick={this.props.onDelete} hidden={this.props.hideDelete} />
        </div>
      </div>
    );
  }
});

});
