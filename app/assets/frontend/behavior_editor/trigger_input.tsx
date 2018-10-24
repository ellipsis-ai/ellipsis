import * as React from 'react';
import debounce from 'javascript-debounce';
import { Picker } from 'emoji-mart';
import { Emoji } from 'emoji-mart';
import DeleteButton from '../shared_ui/delete_button';
import HelpButton from '../help/help_button';
import FormInput from '../form/input';
import Collapsible from '../shared_ui/collapsible';
import ToggleGroup from '../form/toggle_group';
import Trigger, {TriggerInterface} from '../models/trigger';
import TriggerType from '../models/trigger_type';
import Button from "../form/button";
import autobind from "../lib/autobind";

interface EmojiInterface {
  id: string
  name: string
  colons: string
  text: string
  emoticons: Array<string>
  skin?: Option<number>
  native?: Option<string>
  custom?: Option<boolean>
  imageUrl?: Option<string>
}

interface Props {
  triggerTypes: Array<TriggerType>
  trigger: Trigger
  helpVisible: boolean
  id: string
  onChange: (newTrigger: Trigger) => void
  onDelete: () => void,
  onEnterKey: () => void,
  onHelpClick: () => void
}

interface State {
  validated: boolean
  regexError: string | null,
  showError: boolean,
  isShowingEmojiPicker: boolean
}

const EMOJI_SIZE = 36;

class TriggerInput extends React.Component<Props, State> {
  validateTrigger: () => void;
  input: Option<FormInput>;

  constructor(props) {
    super(props);
    autobind(this);
    this.state = {
      validated: false,
      regexError: null,
      showError: false,
      isShowingEmojiPicker: false
    };
    this.validateTrigger = debounce(this._validateTrigger, 500);
  }

  clearError(): void {
    this.setState({
      regexError: null,
      showError: false
    });
  }

  changeTrigger(props: Partial<TriggerInterface>): void {
    var newTrigger = this.props.trigger.clone(props);
    this.props.onChange(newTrigger);
  }

  onChange<K extends keyof TriggerInterface>(propName: K, newValue: TriggerInterface[K]) {
    var changes: Partial<TriggerInterface> = {};
    changes[propName] = newValue;
    this.changeTrigger(changes);
    this.focus();
  }

  onClickEmoji(emoji: EmojiInterface): void {
    this.onChange('text', emoji.id);
    this.toggleReactionPicker();
  }

  setTriggerType(id: string): void {
    if (this.props.trigger.triggerType !== id) {
      this.changeTrigger({
        triggerType: id
      });
    }
  }

  setNormalPhrase(): void {
    if (this.isRegex()) {
      this.changeTrigger({
        isRegex: false
      });
    }
  }

  isRegex(): boolean {
    return this.props.trigger.isRegex;
  }

  setRegex(): void {
    if (!this.isRegex()) {
      this.changeTrigger({
        isRegex: true
      });
    }
  }

  _validateTrigger(): void {
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
  }

  getHelpForRegexError() {
    var isIllegalRepetitionError = /^Illegal repetition/.test(this.state.regexError || "");
    var containsProbableParamName = /{.+?}/.test(this.state.regexError || "");
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
  }

  isEmpty(): boolean {
    return !this.props.trigger.text;
  }

  toggleError(): void {
    this.setState({ showError: !this.state.showError });
    this.focus();
  }

  focus(): void {
    if (this.input) {
      this.input.focus();
    }
  }

  componentDidMount(): void {
    this.validateTrigger();
  }

  componentDidUpdate(prevProps: Props): void {
    if (this.props.trigger !== prevProps.trigger) {
      this.validateTrigger();
    }
  }

  renderErrorMessage() {
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
  }

  renderMessageInput() {
    return (
      <div>
        <div className="position-relative">
          <FormInput
            ref={(el) => this.input = el}
            className={
              " form-input-borderless " +
              (this.props.trigger.isRegex ? " type-monospace " : "")
            }
            id={this.props.id}
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
  }

  renderSelectedEmoji() {
    const text = this.props.trigger.text;
    if (text) {
      return (
        <Emoji emoji={{ id: this.props.trigger.text, skin: 3 }} size={EMOJI_SIZE} />
      );
    } else {
      return (
        <span className="type-l type-disabled">??</span>
      );
    }
  }

  getEmojiColonText(): string {
    const text = this.props.trigger.text;
    if (text && text.trim().length > 0) {
      return `:${text.trim()}:`;
    } else {
      return "";
    }
  }

  toggleReactionPicker(): void {
    this.setState({
      isShowingEmojiPicker: !this.state.isShowingEmojiPicker
    });
  }

  renderReactionInput() {
    return (
      <div>
        <div className="mtm">
          <Button className="button-block" onClick={this.toggleReactionPicker}>
            <span className="display-inline-block align-m mrm">{this.renderSelectedEmoji()}</span>
            <span className="display-inline-block align-m type-s">{this.getEmojiColonText()}</span>
          </Button>
        </div>
        {this.state.isShowingEmojiPicker ? (
          <div className="popup popup-shadow popup-demoted fade-in">
            <Picker
              set="emojione"
              onClick={this.onClickEmoji}
              title={this.props.trigger.text || "Pick your emoji…"}
              emoji={this.props.trigger.text}
              showPreview={false}
              perLine={12}
            />
          </div>
        ) : null}
      </div>
    );
  }

  renderInput() {
    if (this.props.trigger.triggerType === "ReactionAdded") {
      return this.renderReactionInput();
    } else {
      return this.renderMessageInput();
    }
  }

  renderTriggerTypeToggle() {
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
  }

  renderMatchTypeToggle() {
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
  }

  renderRequiresMentionToggle() {
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
  }

  render() {
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
}

export default TriggerInput;
