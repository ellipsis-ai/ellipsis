define(function(require) {
var React = require('react'),
  debounce = require('javascript-debounce'),
  DeleteButton = require('./delete_button'),
  HelpButton = require('../help/help_button'),
  Input = require('../form/input'),
  Collapsible = require('../collapsible'),
  ToggleGroup = require('../form/toggle_group'),
  DropdownMenu = require('./dropdown_menu'),
  Trigger = require('../models/trigger'),
  SVGSettings = require('../svg/settings');
require('whatwg-fetch');

return React.createClass({
  propTypes: {
    trigger: React.PropTypes.instanceOf(Trigger).isRequired,
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
  },
  onChange: function(propName, newValue) {
    var changes = {};
    changes[propName] = newValue;
    this.changeTrigger(changes);
    this.focus();
  },
  setNormalPhrase: function() {
    if (!this.isNormalPhrase()) {
      this.changeTrigger({
        isRegex: false,
        caseSensitive: false
      });
    }
  },
  isNormalPhrase: function() {
    return !this.props.trigger.isRegex && !this.props.trigger.caseSensitive;
  },
  setCaseSensitivePhrase: function() {
    if (!this.isCaseSensitivePhrase()) {
      this.changeTrigger({
        isRegex: false,
        caseSensitive: true
      });
    }
  },
  isCaseSensitivePhrase: function() {
    return !this.props.trigger.isRegex && this.props.trigger.caseSensitive;
  },
  setCaseInsensitiveRegex: function() {
    if (!this.isCaseInsensitiveRegex()) {
      this.changeTrigger({
        isRegex: true,
        caseSensitive: false
      });
    }
  },
  isCaseInsensitiveRegex: function() {
    return this.props.trigger.isRegex && !this.props.trigger.caseSensitive;
  },
  setCaseSensitiveRegex: function() {
    if (!this.isCaseSensitiveRegex()) {
      this.changeTrigger({
        isRegex: true,
        caseSensitive: true
      });
    }
  },
  isCaseSensitiveRegex: function() {
    return this.props.trigger.isRegex && this.props.trigger.caseSensitive;
  },

  onBlur: function() {
    if (this.props.trigger.hasCaseInsensitiveRegexFlagWhileCaseSensitive()) {
      this.changeTrigger({
        caseSensitive: false,
        text: this.props.trigger.text.replace(/^\(\?i\)/, '')
      });
    }
  },
  validateTrigger: debounce(function() {
    if (!this.props.trigger.text || !this.props.trigger.isRegex) {
      this.clearError();
      return;
    }

    var url = jsRoutes.controllers.BehaviorEditorController.regexValidationErrorsFor(this.props.trigger.text).url;
    fetch(url, { credentials: 'same-origin' })
      .then((response) => response.json())
      .then((json) => {
        var error = (json[0] && json[0][0]) ? json[0][0] : null;
        this.setState({
          validated: true,
          regexError: error,
          showError: !!(this.state.showError && error)
        });
      }).catch(() => {
        // TODO: figure out what to do if there's a request error; for now clear user-visible errors
        this.clearError();
      });
  }, 500),

  getHelpForRegexError: function() {
    var isIllegalRepetitionError = /^Illegal repetition/.test(this.state.regexError);
    var containsProbableParamName = /\{.+?\}/.test(this.state.regexError);
    if (isIllegalRepetitionError && containsProbableParamName) {
      return (
        <div className="mts">
          <p>
            <span><b>Tip:</b> if you want to collect user input in a regex trigger, use capturing parentheses with </span>
            <span>a wildcard pattern. Examples:</span>
          </p>

          <div className="type-monospace mhl">
            <div className="box-code-example mbs">add (\d+) plus (\d+)</div>
            <div className="box-code-example mbm">tell (.+?) something</div>
          </div>

          <p>
            <span>If there are multiple inputs, the order of parentheses will follow the order of inputs you’ve defined.</span>
          </p>
        </div>
      );
    } else {
      return null;
    }
  },

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
    if (this.isCaseSensitiveRegex()) {
      label = "Regex pattern (case-sensitive):";
    } else if (this.isCaseInsensitiveRegex()) {
      label = "Regex pattern (ignore case):";
    } else if (this.isCaseSensitivePhrase()) {
      label = "Case-sensitive phrase:";
    } else {
      label = "Phrase:";
    }
    return label;
  },

  componentDidMount: function() {
    this.validateTrigger();
  },

  componentDidUpdate: function(prevProps) {
    if (this.props.trigger !== prevProps.trigger) {
      this.validateTrigger();
    }
  },

  getDropdownIcon: function() {
    return (<div style={{ height: 16 }}><SVGSettings /></div>);
  },

  renderErrorMessage: function() {
    return (
      <div style={{ marginTop: -4 }} className="border bg-blue-lighter border-blue border-error-top pts phm type-s popup-shadow">
        <div className="position-absolute position-top-right ptxs prxs">
          <HelpButton onClick={this.toggleError} toggled={true} inline={true} />
        </div>
        <div className="prl">
          <b>This regex pattern has an error:</b>
        </div>
        <div className="display-overflow-scroll">
          <pre>{this.state.regexError || "\n\n\n"}</pre>
        </div>
        <div>{this.getHelpForRegexError()}</div>
      </div>
    );
  },

  render: function() {
    return (
      <div className="columns columns-elastic mobile-columns-float mbxl">
        <div className="column column-expand">
          <div>
            <label className="type-label type-weak" htmlFor={this.props.id}>{this.getPrefix()}</label>
            <DropdownMenu
              openWhen={this.props.dropdownIsOpen}
              label={this.getDropdownIcon()}
              labelClassName="button-dropdown-trigger-symbol button-s"
              toggle={this.props.onToggleDropdown}
              menuClassName="width-20"
            >
              <DropdownMenu.Item
                onClick={this.setNormalPhrase}
                label="Normal phrase (ignore case)"
                checkedWhen={this.isNormalPhrase()}
              />
              <DropdownMenu.Item
                onClick={this.setCaseSensitivePhrase}
                label="Case-sensitive phrase"
                checkedWhen={this.isCaseSensitivePhrase()}
              />
              <DropdownMenu.Item
                onClick={this.setCaseInsensitiveRegex}
                label="Regular expression (ignore case)"
                checkedWhen={this.isCaseInsensitiveRegex()}
              />
              <DropdownMenu.Item
                onClick={this.setCaseSensitiveRegex}
                label="Regular expression (case-sensitive)"
                checkedWhen={this.isCaseSensitiveRegex()}
              />
            </DropdownMenu>
          </div>
          <div className="position-relative">
            <Input
              className={
                " form-input-borderless " +
                (this.props.trigger.isRegex ? " type-monospace " : "")
              }
              id={this.props.id}
              ref="input"
              value={this.props.trigger.text}
              placeholder="Add a trigger phrase"
              onChange={this.onChange.bind(this, 'text')}
              onBlur={this.onBlur}
              onEnterKey={this.props.onEnterKey}
            />
            <div className={
              `position-absolute position-z-above position-top-right mts mrxs
              ${this.state.regexError ? "fade-in" : "display-none"}`
            }>
              <button type="button"
                className="button-error button-s button-shrink"
                onClick={this.toggleError}
              >
                <span>{this.state.showError ? "▾" : "▸" }</span>
                <span> Error</span>
              </button>
            </div>
            <Collapsible revealWhen={this.state.showError} className="popup popup-demoted display-limit-width">
              {this.renderErrorMessage()}
            </Collapsible>
          </div>
        </div>
        <div className="column column-shrink align-b display-ellipsis prn mobile-pts mobile-pln pbs">
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
        <div className="column column-shrink align-b">
          <DeleteButton onClick={this.props.onDelete} hidden={this.props.hideDelete} />
        </div>
      </div>
    );
  }
});

});
