import * as React from 'react';
import DeleteButton from '../shared_ui/delete_button';
import FormInput from '../form/input';
import Trigger, {TriggerInterface} from '../models/trigger';
import autobind from "../lib/autobind";
import EmojiInput, {EmojiInterface} from "../form/emoji_input";

interface Props {
  existingTriggerEmojiIds: Array<string>
  trigger: Trigger
  id: string
  onChange: (newTrigger: Trigger) => void
  onDelete: () => void
  isShowingEmojiPicker: boolean
  onToggleEmojiPicker: () => void
}

class ReactionTriggerInput extends React.Component<Props> {
  input: Option<FormInput>;

  constructor(props) {
    super(props);
    autobind(this);
  }

  componentWillReceiveProps(newProps: Props): void {
    if (this.props.isShowingEmojiPicker && !newProps.isShowingEmojiPicker && !newProps.trigger.text) {
      this.props.onDelete();
    }
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

  isEmpty(): boolean {
    return !this.props.trigger.text;
  }

  focus(): void {
    if (this.input) {
      this.input.focus();
    }
  }

  renderEmojiText(): React.ReactElement<HTMLSpanElement> {
    const text = this.props.trigger.text;
    return (
      <span className="display-inline-block align-m type-weak type-s">
        {text && text.trim().length > 0 ? (
          <span className="type-monospace">:{text.trim()}:</span>
        ) : (
          <span className="type-disabled">(None)</span>
        )}
      </span>
    );
  }

  toggleReactionPicker(): void {
    this.props.onToggleEmojiPicker();
  }

  filterExistingEmoji(emoji: EmojiInterface): boolean {
    const shortName = emoji.short_names && emoji.short_names[0];
    if (shortName) {
      return !this.props.existingTriggerEmojiIds.includes(shortName);
    } else {
      return true;
    }
  }

  render() {
    return (
      <div className="border border-light bg-white mrm mbm display-inline-block">
        <div className="columns columns-elastic">
          <div className="column column-expand pvs plm prn">
            <EmojiInput
              onClickEmoji={this.onClickEmoji}
              id={this.props.trigger.text}
              filterEmoji={this.filterExistingEmoji}
              emojiAsText={this.renderEmojiText()}
              pickerVisible={this.props.isShowingEmojiPicker}
              onTogglePicker={this.toggleReactionPicker}
            />
          </div>
          <div className="column column-shrink align-m">
            <DeleteButton onClick={this.props.onDelete} />
          </div>
        </div>
      </div>
    );
  }
}

export default ReactionTriggerInput;
