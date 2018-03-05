import * as React from 'react';
import BehaviorGroup from '../models/behavior_group';
import FormInput from '../form/input';
import Textarea from '../form/textarea';
import autobind from '../lib/autobind';

type Props = {
  group: BehaviorGroup,
  onBehaviorGroupIconChange: (string) => void,
  onBehaviorGroupNameChange: (string) => void,
  onBehaviorGroupDescriptionChange: (string) => void
};

class BehaviorGroupDetailsEditor extends React.PureComponent<Props> {
    props: Props;
    skillName: FormInput | null;
    skillDescription: Textarea | null;

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

    render() {
      return (
        <div>
          <div className="columns columns-elastic">
            <div className="column column-shrink">
              <h5>Emoji</h5>
              <FormInput
                className="form-input-borderless form-input-l type-l mbn width-2"
                placeholder="☺"
                onChange={this.props.onBehaviorGroupIconChange}
                value={this.props.group.icon || ""}
              />
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
              rows={"3"}
            />
          </div>
        </div>
      );
    }
}

export default BehaviorGroupDetailsEditor;

