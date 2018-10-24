import * as React from 'react';
import debounce from 'javascript-debounce';
import 'emoji-mart/css/emoji-mart.css';
import { Picker } from 'emoji-mart';
import { Emoji } from 'emoji-mart';
import DeleteButton from '../shared_ui/delete_button';
import HelpButton from '../help/help_button';
import Input from '../form/input';
import Collapsible from '../shared_ui/collapsible';
import ToggleGroup from '../form/toggle_group';
import Trigger from '../models/trigger';
import TriggerType from '../models/trigger_type';

const TriggerInput = React.createClass({
  propTypes: {
    triggerTypes: React.PropTypes.arrayOf(React.PropTypes.instanceOf(TriggerType)).isRequired,
    trigger: React.PropTypes.instanceOf(Trigger).isRequired,
    matchTypeDropdownIsOpen: React.PropTypes.bool.isRequired,
    triggerTypeDropdownIsOpen: React.PropTypes.bool.isRequired,
    helpVisible: React.PropTypes.bool.isRequired,
    id: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.string
    ]).isRequired,
    onChange: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onEnterKey: React.PropTypes.func.isRequired,
    onHelpClick: React.PropTypes.func.isRequired,
    onToggleMatchTypeDropdown: React.PropTypes.func.isRequired,
    onToggleTriggerTypeDropdown: React.PropTypes.func.isRequired
  },
  emojiSize: 36,
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
  onClickEmoji: function(emoji) {
    this.onChange('text', emoji.id);
    this.toggleReactionPicker();
  },
  setTriggerType: function(id) {
    if (this.props.trigger.triggerType !== id) {
      this.changeTrigger({
        triggerType: id
      });
    }
  },
  setNormalPhrase: function() {
    if (this.isRegex()) {
      this.changeTrigger({
        isRegex: false
      });
    }
  },
  isRegex: function() {
    return this.props.trigger.isRegex;
  },
  setRegex: function() {
    if (!this.isRegex()) {
      this.changeTrigger({
        isRegex: true
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
    var isIllegalRepetitionError = /^Illegal repetition/.test(this.state.regexError || "");
    var containsProbableParamName = /\{.+?\}/.test(this.state.regexError || "");
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
    if (this.refs.input) {
      this.refs.input.focus();
    }
  },

  getPrefix: function() {
    return this.isRegex() ? "Regex pattern" : "Phrase";
  },

  componentDidMount: function() {
    this.validateTrigger();
  },

  componentDidUpdate: function(prevProps) {
    if (this.props.trigger !== prevProps.trigger) {
      this.validateTrigger();
    }
  },

  getTriggerTypeDisplayString: function() {
    const found = this.props.triggerTypes.find(ea => ea.id === this.props.trigger.triggerType);
    return found ? found.displayString : "MessageSent";
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

  renderMessageInput: function() {
    return (
      <div>
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
    );
  },

  renderSelectedEmoji: function() {
    return (
      <Emoji emoji={{ id: this.props.trigger.text, skin: 3 }} size={this.emojiSize} />
    );
  },

  getEmojiColonText: function() {
    const text = this.props.trigger.text;
    if (text && text.trim().length > 0) {
      return `:${text.trim()}:`;
    } else {
      return "";
    }
  },

  toggleReactionPicker: function() {
    this.setState({
      isShowingEmojiPicker: !this.state.isShowingEmojiPicker
    });
  },

  renderReactionInput: function() {
    return (
      <div>
        <div className="mtm">
          <a onClick={this.toggleReactionPicker}>
            {this.renderSelectedEmoji()}
            <span className="type-ml height-full align-m">{this.getEmojiColonText()}</span>
          </a>
        </div>
        <Collapsible revealWhen={this.state.isShowingEmojiPicker} className="popup popup-demoted display-limit-width">
          <Picker
            set="emojione"
            onClick={this.onClickEmoji}
            title={this.props.trigger.text || "Pick your emoji…"}
            emoji={this.props.trigger.text}
            showPreview={false}
            perLine={12}
          />
        </Collapsible>
      </div>
    );
  },

  renderInput: function() {
    if (this.props.trigger.triggerType === "ReactionAdded") {
      return this.renderReactionInput();
    } else {
      return this.renderMessageInput();
    }
  },

  renderTriggerTypeToggle: function() {
    return (
      <ToggleGroup className="form-toggle-group-s align-m">
        {this.props.triggerTypes.map(ea => {
          return (
            <ToggleGroup.Item
              key={`triggerType${ea.id}`}
              onClick={() => this.setTriggerType(ea.id)}
              label={ea.displayString}
              activeWhen={this.props.trigger.triggerType === ea.id}
            />
          );
        })}
      </ToggleGroup>
    );
  },

  renderMatchTypeToggle: function() {
    if (this.props.trigger.triggerType === "ReactionAdded") {
      return null;
    } else {
      return (
        <ToggleGroup className="form-toggle-group-s align-m mll">
          <ToggleGroup.Item
            onClick={this.setNormalPhrase}
            label="Normal phrase"
            activeWhen={!this.isRegex()}
          />
          <ToggleGroup.Item
            onClick={this.setRegex}
            label="Regular expression"
            activeWhen={this.isRegex()}
          />
        </ToggleGroup>
      );
    }
  },

  renderRequiresMentionToggle: function() {
    if (this.props.trigger.triggerType === "ReactionAdded") {
      return null;
    } else {
      return (
        <div className="column column-shrink align-b display-ellipsis prn mobile-pts mobile-pln pbs">
          <ToggleGroup className="form-toggle-group-s align-m">
            <ToggleGroup.Item
              title="Ellipsis will only respond when mentioned, or when a message begins with three periods
                “…”."
              label="To Ellipsis"
              activeWhen={this.props.trigger.requiresMention}
              onClick={this.onChange.bind(this, 'requiresMention', true)}
            />
            <ToggleGroup.Item
              title="Ellipsis will respond to any message with this phrase"
              label="Any message"
              activeWhen={!this.props.trigger.requiresMention}
              onClick={this.onChange.bind(this, 'requiresMention', false)}
            />
          </ToggleGroup>
        </div>
      );
    }
  },

  render: function() {
    return (
      <div className="border border-light bg-white plm pbm">
        <div className="columns columns-elastic mobile-columns-float">
          <div className="column column-expand ptxs">
            <div>
              {this.renderTriggerTypeToggle()}
              {this.renderMatchTypeToggle()}
            </div>
            {this.renderInput()}
          </div>
          {this.renderRequiresMentionToggle()}
          <div className="column column-shrink align-t">
            <DeleteButton onClick={this.props.onDelete} />
          </div>
        </div>
      </div>
    );
  }
});

export default TriggerInput;
