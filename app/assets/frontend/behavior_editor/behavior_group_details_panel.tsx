import * as React from 'react';
import BehaviorGroup from '../models/behavior_group';
import BehaviorGroupDetailsEditor from './behavior_group_details_editor';
import Button from '../form/button';
import autobind from '../lib/autobind';

type Props = {
    group: BehaviorGroup,
    onBehaviorGroupIconChange: (string) => void,
    onBehaviorGroupNameChange: (string) => void,
    onBehaviorGroupDescriptionChange: (string) => void,
    onDone: () => void,
    visible: boolean
};

class BehaviorGroupDetailsPanel extends React.Component<Props> {
    props: Props;
    detailsEditor: Option<BehaviorGroupDetailsEditor>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.detailsEditor = null;
    }

    componentDidUpdate(prevProps: Props) {
      if (!prevProps.visible && this.props.visible) {
        this.focus();
      }
    }

    focus(): void {
      if (this.detailsEditor) {
        this.detailsEditor.focus();
      }
    }

    render() {
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak mtn">Skill details</h4>
              </div>
              <div className="column column-page-main">

                <p>
                  <span>Enter a title for this skill.</span>
                </p>

                <p>
                  <span>The title will be shown when users ask for help, and wherever the team’s </span>
                  <span>skills are listed.</span>
                </p>

                <p>
                  <span>You can also add an emoji to help people more easily identify the skill, and write a description </span>
                  <span>to tell people what the skill can do in more detail.</span>
                </p>

                <BehaviorGroupDetailsEditor
                  ref={(el) => this.detailsEditor = el}
                  group={this.props.group}
                  onBehaviorGroupNameChange={this.props.onBehaviorGroupNameChange}
                  onBehaviorGroupDescriptionChange={this.props.onBehaviorGroupDescriptionChange}
                  onBehaviorGroupIconChange={this.props.onBehaviorGroupIconChange}
                />

                <div className="mtxl">
                  <Button onClick={this.props.onDone}
                    className="button-primary"
                    disabled={!this.props.group.name}
                  >Done</Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
}

export default BehaviorGroupDetailsPanel;

