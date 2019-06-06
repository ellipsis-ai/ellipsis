import * as React from 'react';
import {Picker, Emoji, EmojiData} from 'emoji-mart';
import autobind from '../lib/autobind';
import DropdownContainer from "../shared_ui/dropdown_container";
import Button from "./button";

export const EMOJI_SIZE = 32;
export const EMOJI_SHEET_SIZE = 64;
export const EMOJI_SET = "twitter";

interface Props {
  id?: string
  raw?: Option<string>
  pickerVisible: boolean
  onTogglePicker: () => void
  onClickEmoji: (emoji: EmojiData) => void
  filterEmoji?: (emoji: EmojiData) => boolean
  emojiAsText?: React.ReactNode
}

class EmojiInput extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  renderSelectedEmoji() {
    const emoji = this.props.id ? this.props.id : null;
    if (emoji) {
      return (
        <Emoji
          native={true}
          emoji={emoji}
          size={EMOJI_SIZE}
          sheetSize={EMOJI_SHEET_SIZE}
          set={EMOJI_SET}
        />
      );
    } else if (this.props.raw) {
      return (
        <span className="type-l">{this.props.raw}</span>
      )
    } else {
      return (
        <span className="type-icon type-disabled">?</span>
      );
    }
  }

  render() {
    return (
      <div>
        {this.props.pickerVisible ? (
          <DropdownContainer>
            <div className="popup popup-shadow popup-demoted">
              <Picker
                exclude={["recent"]}
                native={true}
                set={EMOJI_SET}
                onClick={this.props.onClickEmoji}
                sheetSize={EMOJI_SHEET_SIZE}
                emojiSize={24}
                autoFocus={true}
                title={this.props.id || "Pick your emoji…"}
                emoji={this.props.id}
                showPreview={false}
                perLine={8}
                emojisToShowFilter={this.props.filterEmoji}
              />
            </div>
          </DropdownContainer>
        ) : null}
        <Button className="button-dropdown-trigger button-dropdown-trigger-borderless pvn" onClick={this.props.onTogglePicker} stopPropagation={true}>
          <span className="display-inline-block align-m mrm height-icon width-icon align-c">{this.renderSelectedEmoji()}</span>
          {this.props.emojiAsText}
        </Button>
      </div>
    );
  }
}

export default EmojiInput;
