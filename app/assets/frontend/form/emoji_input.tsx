import * as React from 'react';
import { Picker, Emoji } from 'emoji-mart';
import autobind from '../lib/autobind';
import DropdownContainer from "../shared_ui/dropdown_container";
import Button from "./button";

const EMOJI_SIZE = 32;
const EMOJI_SHEET_SIZE = 64;
const EMOJI_SET = "twitter";

export interface EmojiInterface {
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
  value: string
  pickerVisible: boolean
  onTogglePicker: () => void
  onClickEmoji: (emoji: EmojiInterface) => void
  filterEmoji?: (emoji: EmojiInterface) => boolean
  emojiAsText?: React.ReactNode
}

class EmojiInput extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  renderSelectedEmoji() {
    const text = this.props.value;
    if (text) {
      return (
        <Emoji
          native={true}
          emoji={{ id: text, skin: 3 }}
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
                title={this.props.value || "Pick your emoji…"}
                emoji={this.props.value}
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
