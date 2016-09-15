define(function(require) {
var React = require('react'),
  debounce = require('javascript-debounce'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  DeleteButton = require('./delete_button'),
  HelpButton = require('../help/help_button'),
  Input = require('../form/input'),
  Collapsible = require('../collapsible'),
  ToggleGroup = require('../form/toggle_group'),
  DropdownMenu = require('./dropdown_menu');
  require('whatwg-fetch');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    caseSensitive: React.PropTypes.bool.isRequired,
    large: React.PropTypes.bool,
    dropdownIsOpen: React.PropTypes.bool.isRequired,
    helpVisible: React.PropTypes.bool.isRequired,
    hideDelete: React.PropTypes.bool.isRequired,
    id: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.string
    ]).isRequired,
    isRegex: React.PropTypes.bool.isRequired,
    onChange: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEnterKey: React.PropTypes.func.isRequired,
    onHelpClick: React.PropTypes.func.isRequired,
    onToggleDropdown: React.PropTypes.func.isRequired,
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
  toggleCaseSensitive: function() {
    this.onChange('caseSensitive', !this.props.caseSensitive);
  },
  toggleIsRegex: function() {
    this.onChange('isRegex', !this.props.isRegex);
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

    var url = jsRoutes.controllers.BehaviorEditorController.regexValidationErrorsFor(this.props.value).url;
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
  focus: function() {
    this.refs.input.focus();
  },

  getPrefix: function() {
    var label;
    if (this.props.caseSensitive && this.props.isRegex) {
      label = "Case-sensitive regex pattern:";
    } else if (this.props.caseSensitive) {
      label = "Case-sensitive phrase:";
    } else if (this.props.isRegex) {
      label = "Case-insensitive regex pattern:";
    } else {
      label = "Phrase:";
    }
    return label;
  },

  componentDidMount: function() {
    if (this.props.isRegex) {
      this.validateTrigger();
    }
  },

  render: function() {
    return (
      <div className="columns columns-elastic mobile-columns-float mbm mobile-mbxl">
        <div className="column column-expand">
          <div className="columns columns-elastic">
            <div className="column column-shrink align-m ptxs prn">
              <DropdownMenu
                openWhen={this.props.dropdownIsOpen}
                label={this.getPrefix()}
                labelClassName="button-dropdown-trigger-borderless button-s mrs type-label type-weak"
                toggle={this.props.onToggleDropdown}
              >
                <DropdownMenu.Item
                  onClick={this.toggleCaseSensitive}
                  label="Case-sensitive"
                  checkedWhen={this.props.caseSensitive}
                />
                <DropdownMenu.Item
                  onClick={this.toggleIsRegex}
                  label="Regular expression pattern"
                  checkedWhen={this.props.isRegex}
                />
              </DropdownMenu>
            </div>
            <div className="column column-expand prn position-relative">
              <Input
                className={
                  " form-input-borderless " +
                  (this.props.isRegex ? " type-monospace " : "") +
                  (this.props.large ? " form-input-large " : "")
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
              activeWhen={!this.props.requiresMention}
              onClick={this.onChange.bind(this, 'requiresMention', false)}
            />
            <ToggleGroup.Item
              title="Ellipsis will only respond when mentioned, or when a message begins with three periods
              “…”."
              label="To Ellipsis"
              activeWhen={this.props.requiresMention}
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
