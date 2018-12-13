import * as React from 'react';
import AddButton from '../form/add_button';
import EditableName from '../../frontend/behavior_list/editable_name';
import Editable from '../models/editable';
import Sort from '../lib/sort';
import {ReactNode} from "react";
import {BehaviorSelectCallback} from "./behavior_switcher";

type Props = {
  heading: string,
  editables: Array<Editable>,
  selectedId?: Option<string>,
  onAddNew: () => void,
  addNewLabel?: Option<string>,
  onSelect: BehaviorSelectCallback,
  isModified: (e: Editable) => boolean,
  renderGroupStatus?: () => ReactNode,
  renderEditableStatus?: (e: Editable) => ReactNode
}

class BehaviorSwitcherGroup extends React.Component<Props> {
    getSelected(): Editable | undefined {
      return this.props.editables.find((ea) => Boolean(this.props.selectedId) && ea.getPersistentId() === this.props.selectedId );
    }

    getEditableList(): Array<Editable> {
      return Sort.arrayAlphabeticalBy(this.props.editables, ea => ea.sortKey());
    }

    isSelected(editable: Editable): boolean {
      const selected = this.getSelected();
      return Boolean(selected && editable.getPersistentId() === selected.getPersistentId());
    }

    renderNameFor(editable: Editable) {
      return (
        <EditableName
          className="plxl mobile-pll"
          triggerClassName={this.isSelected(editable) ? "box-chat-selected" : "opacity-75"}
          version={editable}
          disableLink={this.isSelected(editable)}
          omitDescription={true}
          onClick={this.props.onSelect}
          renderStatus={this.props.renderEditableStatus}
        />
      );
    }

    renderStatus(): ReactNode {
      if (this.props.renderGroupStatus) {
        return this.props.renderGroupStatus();
      } else {
        return null;
      }
    }

    render() {
      const editables = this.getEditableList();
      const hasEditables = editables.length > 0;
      return (
        <div className="border-bottom pbl">
          <div className="container container-wide prl">
            <div className="columns columns-elastic">
              <div className="column column-expand ptl">
                <h6>{this.props.heading}{this.renderStatus()}</h6>
              </div>
              <div className="column column-shrink ptm type-link">
                <AddButton
                  onClick={this.props.onAddNew}
                  label={this.props.addNewLabel}
                />
              </div>
            </div>
          </div>
          <div className={`type-s ${hasEditables ? "mts" : ""}`}>
            {editables.map((editable, index) => (
              <div
                key={`behavior${index}`}
                className={`pvxs ${this.isSelected(editable) ? "bg-blue border-blue-medium type-white" : ""}`}
              >
                <div className={"position-absolute position-left pls type-bold type-m " + (this.isSelected(editable) ? "" : "type-pink")}>
                  {this.props.isModified(editable) ? "•" : ""}
                </div>
                {this.renderNameFor(editable)}
              </div>
            ))}
          </div>
        </div>
      );
    }
}

export default BehaviorSwitcherGroup;

