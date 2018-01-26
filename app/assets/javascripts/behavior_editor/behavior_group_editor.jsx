// @flow
define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    Button = require('../form/button'),
    BehaviorGroupDetailsEditor = require('./behavior_group_details_editor'),
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
      this.detailsEditor = null;
    }

    focus(): void {
      if (this.detailsEditor) {
        this.detailsEditor.focus();
      }
    }

    exportGroup(): void {
      window.location = jsRoutes.controllers.BehaviorImportExportController.export(this.props.group.id).url;
    }

    render(): React.Node {
      return (
        <div>
          <div className="container container-narrow mtl">
            <BehaviorGroupDetailsEditor
              ref={(el) => this.detailsEditor = el}
              group={this.props.group}
              onBehaviorGroupNameChange={this.props.onBehaviorGroupNameChange}
              onBehaviorGroupDescriptionChange={this.props.onBehaviorGroupDescriptionChange}
              onBehaviorGroupIconChange={this.props.onBehaviorGroupIconChange}
            />
          </div>

          <hr className="mvxxl rule-subtle" />

          <div className="container container-narrow mbxl">

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

