import * as React from 'react';
import BehaviorGroup from '../models/behavior_group';
import Button from '../form/button';
import BehaviorGroupDetailsEditor from './behavior_group_details_editor';
import autobind from '../lib/autobind';

type Props = {
    group: BehaviorGroup,
    isModified: boolean,
    onBehaviorGroupNameChange: (s: string) => void,
    onBehaviorGroupDescriptionChange: (s: string) => void,
    onBehaviorGroupIconChange: (s: string) => void,
    onDeleteClick: () => void
    iconPickerVisible: boolean
    onToggleIconPicker: () => void
}

class BehaviorGroupEditor extends React.PureComponent<Props> {
    props: Props;
    detailsEditor: Option<BehaviorGroupDetailsEditor>;

    constructor(props: Props) {
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
      if (this.props.group.id) {
        window.location.href = jsRoutes.controllers.BehaviorImportExportController.export(this.props.group.id).url;
      }
    }

    render() {
      return (
        <div>
          <div className="container container-narrow mtl">
            <BehaviorGroupDetailsEditor
              ref={(el) => this.detailsEditor = el}
              group={this.props.group}
              onBehaviorGroupNameChange={this.props.onBehaviorGroupNameChange}
              onBehaviorGroupDescriptionChange={this.props.onBehaviorGroupDescriptionChange}
              onBehaviorGroupIconChange={this.props.onBehaviorGroupIconChange}
              iconPickerVisible={this.props.iconPickerVisible}
              onToggleIconPicker={this.props.onToggleIconPicker}
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

export default BehaviorGroupEditor;


