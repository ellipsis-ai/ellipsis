define(function(require) {
var React = require('react'),
  debounce = require('lodash.debounce'),
  ES6Promise = require('es6-promise'),
  Fetch = require('fetch'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorCheckbox = require('./behavior_editor_checkbox'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorHelpButton = require('./behavior_editor_help_button'),
  BehaviorEditorInput = require('./behavior_editor_input'),
  Collapsible = require('./collapsible');

return React.createClass({
  displayName: 'BehaviorEditorTriggerInput',
  mixins: [BehaviorEditorMixin],
  getInitialState: function() {
    return {
      highlightCaseSensitivity: false,
      regexError: null,
      showError: false,
      showHelp: false
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

    var url = '/regex_validation_errors/' + encodeURIComponent(this.props.value);
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
      }.bind(this)).catch(function(ex) {
        // TODO: figure out what to do if there's a request error; for now clear user-visible errors
        this.clearError();
      });
  }, 500),
  isEmpty: function() {
    return !this.props.value;
  },
  toggleError: function(event) {
    this.setState({ showError: !this.state.showError });
    this.focus();
  },
  toggleHelp: function() {
    this.setState({ showHelp: !this.state.showHelp });
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
      <div className="columns columns-elastic mbs">
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
          <BehaviorEditorInput
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
                <BehaviorEditorHelpButton onClick={this.toggleError} toggled={true} inline={true} />
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
        <div className="column column-shrink prn position-relative">
          <div className={"display-ellipsis form-input form-input-borderless " +
            (this.props.className || "")}>
            {this.props.includeHelp ? (
              <BehaviorEditorHelpButton onClick={this.toggleHelp} toggled={this.state.showHelp} className="align-m mrs" />
              ) : ""}
            <label className="mrm type-s" title="Only respond when someone mentions @ellipsis">
              <BehaviorEditorCheckbox
                checked={this.props.requiresMention}
                onChange={this.onChange.bind(this, 'requiresMention')}
              /> <span>🗣 🤖</span>
            </label>
            <label
              className={"mrm type-s " + (this.state.highlightCaseSensitivity ? "blink-twice" : "")}
              title="Match uppercase and lowercase letters exactly — if unchecked, case is ignored"
            >
              <BehaviorEditorCheckbox
                checked={this.props.caseSensitive}
                onChange={this.onChange.bind(this, 'caseSensitive')}
              /> <i>Aa</i>
            </label>
            <label className="type-s" title="Use regular expression pattern matching">
              <BehaviorEditorCheckbox
                checked={this.props.isRegex}
                onChange={this.onChange.bind(this, 'isRegex')}
              /> <code>/^…$/</code>
            </label>
          </div>
          <Collapsible revealWhen={this.state.showHelp} className="popup display-limit-width">
            <div className="border bg-blue-lighter border-blue border-error-top pts phm type-xs popup-shadow">
              <p>
                🗣 🤖: if checked, only respond to this trigger if someone mentions
                Ellipsis by name.
              </p>
              <p>
                <i>Aa</i>: if checked, only respond to this trigger if someone
                uses the same capitalization.
              </p>
              <p>
                <code>/^…$/</code>: if checked, process this trigger as a regular
                expression pattern (regex) instead of normal text. Use regex capturing parentheses
                to collect user input instead of the <code>{"{paramName}"}</code> style.
              </p>
            </div>
          </Collapsible>
        </div>
        <div className="column column-shrink">
          <BehaviorEditorDeleteButton onClick={this.props.onDelete} hidden={this.props.hideDelete} />
        </div>
      </div>
    );
  }
});

});
