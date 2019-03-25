import * as React from 'react';
import BehaviorGroup from '../models/behavior_group';
import FormInput from '../form/input';
import Textarea from '../form/textarea';
import autobind from '../lib/autobind';
import EmojiInput from "../form/emoji_input";
import DeleteButton from "../shared_ui/delete_button";
import {EmojiData} from "emoji-mart";
import {BaseEmoji} from "emoji-mart/dist-es/utils/emoji-index/nimble-emoji-index";

interface Props {
  group: BehaviorGroup,
  onBehaviorGroupIconChange: (icon: string) => void,
  onBehaviorGroupNameChange: (name: string) => void,
  onBehaviorGroupDescriptionChange: (description: string) => void
  iconPickerVisible: boolean
  onToggleIconPicker: () => void
}

class BehaviorGroupDetailsEditor extends React.PureComponent<Props> {
    props: Props;
    skillName: Option<FormInput>;
    skillDescription: Option<Textarea>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.skillDescription = null;
      this.skillName = null;
    }

    focus(): void {
      if (this.props.group.name && this.skillDescription) {
        this.skillDescription.focus();
      } else if (this.skillName) {
        this.skillName.focus();
      }
    }

    onChangeEmoji(emoji: EmojiData): void {
      const baseEmoji = emoji as BaseEmoji;
      this.props.onBehaviorGroupIconChange(baseEmoji.native || "");
      this.props.onToggleIconPicker();
    }

    onDeleteEmoji(): void {
      this.props.onBehaviorGroupIconChange("");
    }

    render() {
      return (
        <div>
          <div className="columns columns-elastic">
            <div className="column column-shrink">
              <h5>Emoji</h5>
              <div className="pts">
                <div className="columns columns-elastic">
                  <div className="column column-expand prn">
                    <EmojiInput raw={this.props.group.icon} pickerVisible={this.props.iconPickerVisible} onTogglePicker={this.props.onToggleIconPicker} onClickEmoji={this.onChangeEmoji} />
                  </div>
                  <div className="column column-shrink align-m">
                    <DeleteButton className="button-s" onClick={this.onDeleteEmoji} title="Remove icon" />
                  </div>
                </div>
              </div>
            </div>
            <div className="column column-expand">
              <h5>Title</h5>
              <FormInput
                ref={(el) => this.skillName = el}
                className="form-input-borderless form-input-l type-l type-semibold mbn width-20"
                placeholder="Untitled"
                onChange={this.props.onBehaviorGroupNameChange}
                value={this.props.group.name || ""}
              />
            </div>
          </div>

          <h5 className="mtxxl mbs">Description</h5>
          <div>
            <Textarea
              ref={(el) => this.skillDescription = el}
              className="form-input-height-auto"
              placeholder="Describe the general purpose of this skill. The description is displayed in help."
              onChange={this.props.onBehaviorGroupDescriptionChange}
              value={this.props.group.description || ""}
              rows={3}
            />
          </div>
        </div>
      );
    }
}

export default BehaviorGroupDetailsEditor;

