define(function(require) {
var React = require('react'),
  debounce = require('javascript-debounce'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  DeleteButton = require('./delete_button'),
  HelpButton = require('../help/help_button'),
  Input = require('../form/input'),
  Collapsible = require('../collapsible'),
  ToggleGroup = require('../form/toggle_group'),
  DropdownMenu = require('./dropdown_menu'),
  Trigger = require('../models/trigger');
require('whatwg-fetch');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    trigger: React.PropTypes.instanceOf(Trigger).isRequired,
    large: React.PropTypes.bool,
    dropdownIsOpen: React.PropTypes.bool.isRequired,
    helpVisible: React.PropTypes.bool.isRequired,
    hideDelete: React.PropTypes.bool.isRequired,
    id: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.string
    ]).isRequired,
    onChange: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEnterKey: React.PropTypes.func.isRequired,
    onHelpClick: React.PropTypes.func.isRequired,
    onToggleDropdown: React.PropTypes.func.isRequired
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
    var newTrigger = this.props.trigger.clone(props);
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
  toggleCaseSensitive: function() {
    this.onChange('caseSensitive', !this.props.trigger.caseSensitive);
  },
  toggleIsRegex: function() {
    this.onChange('isRegex', !this.props.trigger.isRegex);
  },
  onBlur: function() {
    if (this.props.trigger.hasCaseInsensitiveRegexFlagWhileCaseSensitive()) {
      this.changeTrigger({
        caseSensitive: false,
        text: this.props.trigger.text.replace(/^\(\?i\)/, '')
      });
      this.setState({ highlightCaseSensitivity: true });
      window.setTimeout(() => { this.setState({ highlightCaseSensitivity: false }); }, 1000);
    }
  },
  validateTrigger: debounce(function() {
    if (!this.props.trigger.text) {
      this.clearError();
      return;
    }

    var url = jsRoutes.controllers.BehaviorEditorController.regexValidationErrorsFor(this.props.trigger.text).url;
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
    return !this.props.trigger.text;
  },
  toggleError: function() {
    this.setState({ showError: !this.state.showError });
    this.focus();
  },
  focus: function() {
    this.refs.input.focus();
  },

  getPrefix: function() {
    var label;
    if (this.props.trigger.caseSensitive && this.props.trigger.isRegex) {
      label = "Case-sensitive regex pattern:";
    } else if (this.props.trigger.caseSensitive) {
      label = "Case-sensitive phrase:";
    } else if (this.props.trigger.isRegex) {
      label = "Case-insensitive regex pattern:";
    } else {
      label = "Phrase:";
    }
    return label;
  },

  componentDidMount: function() {
    if (this.props.trigger.isRegex) {
      this.validateTrigger();
    }
  },

  render: function() {
    return (
      <div className="columns columns-elastic mobile-columns-float mbm mobile-mbxl">
        <div className="column column-expand">
          <div className="columns columns-elastic">
            <div className="column column-shrink align-m prn">
              <DropdownMenu
                openWhen={this.props.dropdownIsOpen}
                label={this.getPrefix()}
                labelClassName="button-dropdown-trigger-borderless button-s mrs type-label type-weak"
                toggle={this.props.onToggleDropdown}
              >
                <DropdownMenu.Item
                  onClick={this.toggleCaseSensitive}
                  label="Case-sensitive"
                  checkedWhen={this.props.trigger.caseSensitive}
                />
                <DropdownMenu.Item
                  onClick={this.toggleIsRegex}
                  label="Regular expression pattern"
                  checkedWhen={this.props.trigger.isRegex}
                />
              </DropdownMenu>
            </div>
            <div className="column column-expand prn position-relative">
              <Input
                className={
                  " form-input-borderless " +
                  (this.props.trigger.isRegex ? " type-monospace " : "") +
                  (this.props.large ? " form-input-large " : "")
                }
                id={this.props.id}
                ref="input"
                value={this.props.trigger.text}
                placeholder="Add a trigger phrase"
                onChange={this.onChange.bind(this, 'text')}
                onBlur={this.onBlur}
                onEnterKey={this.props.onEnterKey}
              />
              {this.state.regexError ? (
                <div className="position-absolute position-z-above position-top-right mts mrxs fade-in">
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
                  <div className="prl">
                    <b>This regex pattern has an error:</b>
                  </div>
                  <pre>{this.state.regexError || "\n\n\n"}</pre>
                </div>
              </Collapsible>
            </div>
          </div>
        </div>
        <div className={
          "column column-shrink prn display-ellipsis mobile-pts " +
          (this.props.large ? " ptm " : " pts ")
        }>
          <ToggleGroup className="form-toggle-group-s align-m">
            <ToggleGroup.Item
              title="Ellipsis will respond to any message with this phrase"
              label="Any message"
              activeWhen={!this.props.trigger.requiresMention}
              onClick={this.onChange.bind(this, 'requiresMention', false)}
            />
            <ToggleGroup.Item
              title="Ellipsis will only respond when mentioned, or when a message begins with three periods
              “…”."
              label="To Ellipsis"
              activeWhen={this.props.trigger.requiresMention}
              onClick={this.onChange.bind(this, 'requiresMention', true)}
            />
          </ToggleGroup>
        </div>
        <div className="column column-shrink">
          <DeleteButton onClick={this.props.onDelete} hidden={this.props.hideDelete} />
        </div>
      </div>
    );
  }
});

});
