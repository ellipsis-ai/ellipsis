// @flow
define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    Button = require('../form/button'),
    FormInput = require('../form/input'),
    Textarea = require('../form/textarea'),
    autobind = require('../lib/autobind');

  type Props = {
    group: BehaviorGroup,
    isModified: boolean,
    onBehaviorGroupNameChange: (string) => void,
    onBehaviorGroupDescriptionChange: (string) => void,
    onBehaviorGroupIconChange: (string) => void,
    onDeleteClick: () => void
  }

  class BehaviorGroupEditor extends React.PureComponent<Props> {
    props: Props;

    constructor(props) {
      super(props);
      autobind(this);
      this.skillDescription = null;
      this.skillName = null;
    }

    focus(): void {
      if (this.props.group.name) {
        this.skillDescription.focus();
      } else {
        this.skillName.focus();
      }
    }

    exportGroup(): void {
      window.location = jsRoutes.controllers.BehaviorImportExportController.export(this.props.group.id).url;
    }

    render(): React.Node {
      return (
        <div>
          <div className="container container-narrow mtl">

            <div className="columns columns-elastic">
              <div className="column column-shrink">
                <h5>Icon</h5>
                <FormInput
                  className="form-input-borderless form-input-l type-l mbn width-2"
                  placeholder="Icon"
                  onChange={this.props.onBehaviorGroupIconChange}
                  value={this.props.group.icon || ""}
                />
              </div>
              <div className="column column-expand">
                <h5>Title</h5>
                <FormInput
                  ref={(el) => this.skillName = el}
                  className="form-input-borderless form-input-l type-l type-semibold mbn width-20"
                  placeholder="Add a title (optional)"
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
                placeholder="Describe the general purpose of this skill (optional). The description is displayed in help."
                onChange={this.props.onBehaviorGroupDescriptionChange}
                value={this.props.group.description || ""}
                rows={"3"}
              />
            </div>

          </div>

          <hr className="mvxxl rule-subtle" />

          <div className="container container-narrow">

            <div className="columns columns-elastic mobile-columns-float">
              <div className="column column-expand mobile-mbm">
                <Button
                  className="mrs"
                  onClick={this.exportGroup}
                  disabled={this.props.isModified}
                >
                  Export skill as ZIP file
                </Button>
              </div>

              <div className="column column-shrink">
                <Button
                  className={"button-shrink"}
                  onClick={this.props.onDeleteClick}
                  disabled={this.props.isModified}
                >
                  Delete entire skill…
                </Button>
              </div>
            </div>
          </div>
        </div>


      );
    }
  }

  return BehaviorGroupEditor;
});

