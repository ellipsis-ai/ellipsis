import * as React from 'react';
import { Picker } from 'emoji-mart';
import { Emoji } from 'emoji-mart';
import DeleteButton from '../shared_ui/delete_button';
import FormInput from '../form/input';
import Trigger, {TriggerInterface} from '../models/trigger';
import Button from "../form/button";
import autobind from "../lib/autobind";
import DropdownContainer from "../shared_ui/dropdown_container";

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
  short_names?: Array<string>
}

interface Props {
  existingTriggerEmojiIds: Array<string>
  trigger: Trigger
  id: string
  onChange: (newTrigger: Trigger) => void
  onDelete: () => void
  isShowingEmojiPicker: boolean
  onToggleEmojiPicker: () => void
}

const EMOJI_SIZE = 32;
const EMOJI_SHEET_SIZE = 64;
const EMOJI_SET = "twitter";

class ReactionTriggerInput extends React.Component<Props> {
  input: Option<FormInput>;

  constructor(props) {
    super(props);
    autobind(this);
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

  renderSelectedEmoji() {
    const text = this.props.trigger.text;
    if (text) {
      return (
        <Emoji
          native={true}
          emoji={{ id: this.props.trigger.text, skin: 3 }}
          size={EMOJI_SIZE}
          sheetSize={EMOJI_SHEET_SIZE}
          set={EMOJI_SET}
        />
      );
    } else {
      return (
        <span className="type-icon type-disabled">?</span>
      );
    }
  }

  renderEmojiText(): React.ReactElement<HTMLSpanElement> {
    const text = this.props.trigger.text;
    if (text && text.trim().length > 0) {
      return (
        <span className="type-monospace">:{text.trim()}:</span>
      );
    } else {
      return (
        <span className="type-disabled">(None)</span>
      );
    }
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
          <div className="column column-expand pvm plm prn">
            <Button className="button-block" onClick={this.toggleReactionPicker} stopPropagation={true}>
              <span className="display-inline-block align-m mrm height-icon width-icon align-c">{this.renderSelectedEmoji()}</span>
              <span className="display-inline-block align-m type-weak type-s">{this.renderEmojiText()}</span>
            </Button>
            {this.props.isShowingEmojiPicker ? (
              <DropdownContainer>
                <div className="popup popup-shadow popup-demoted">
                  <Picker
                    native={true}
                    set={EMOJI_SET}
                    onClick={this.onClickEmoji}
                    sheetSize={EMOJI_SHEET_SIZE}
                    emojiSize={24}
                    autoFocus={true}
                    title={this.props.trigger.text || "Pick your emoji…"}
                    emoji={this.props.trigger.text}
                    showPreview={false}
                    perLine={12}
                    emojisToShowFilter={this.filterExistingEmoji}
                  />
                </div>
              </DropdownContainer>
            ) : null}
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