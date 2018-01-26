// @flow
define(function(require) {
  const React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    BehaviorGroupDetailsEditor = require('./behavior_group_details_editor'),
    Button = require('../form/button'),
    autobind = require('../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    onBehaviorGroupIconChange: (string) => void,
    onBehaviorGroupNameChange: (string) => void,
    onBehaviorGroupDescriptionChange: (string) => void,
    onDone: () => void
  };

  class BehaviorGroupDetailsPanel extends React.Component<Props> {
    props: Props;
    detailsEditor: ?BehaviorGroupDetailsEditor;

    constructor(props: Props): void {
      super(props);
      autobind(this);
      this.detailsEditor = null;
    }

    focus(): void {
      if (this.detailsEditor) {
        this.detailsEditor.focus();
      }
    }

    render(): React.Node {
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak mtn">Skill details</h4>
              </div>
              <div className="column column-page-main">

                <p>
                  <span>Before you begin, enter a title for this new skill.</span>
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

  return BehaviorGroupDetailsPanel;
});
