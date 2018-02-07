// @flow
define(function(require) {
  const React = require('react'),
    AddButton = require('../form/add_button'),
    EditableName = require('../../frontend/behavior_list/editable_name'),
    Editable = require('../models/editable'),
    Sort = require('../lib/sort');

  type Props = {
    heading: string,
    editables: Array<Editable>,
    selectedId?: string,
    onAddNew: () => void,
    addNewLabel?: string,
    onSelect: (groupId: string, selectableId: string) => void,
    isModified: (Editable) => boolean
  }

  class BehaviorSwitcherGroup extends React.Component<Props> {
    getSelected(): ?Editable {
      return this.props.editables.find(ea => this.props.selectedId && ea.getPersistentId() === this.props.selectedId );
    }

    getEditableList(): Array<Editable> {
      return Sort.arrayAlphabeticalBy(this.props.editables, ea => ea.sortKey());
    }

    isSelected(editable): boolean {
      const selected = this.getSelected();
      return Boolean(selected && editable.getPersistentId() === selected.getPersistentId());
    }

    renderNameFor(editable): React.Node {
      return (
        <EditableName
          className="plxl mobile-pll"
          triggerClassName={this.isSelected(editable) ? "box-chat-selected" : "opacity-75"}
          version={editable}
          disableLink={this.isSelected(editable)}
          omitDescription={true}
          onClick={this.props.onSelect}
        />
      );
    }

    render(): React.Node {
      const editables = this.getEditableList();
      const hasEditables = editables.length > 0;
      return (
        <div className="border-bottom pbl">
          <div className="container container-wide prl">
            <div className="columns columns-elastic">
              <div className="column column-expand ptl">
                <h6>{this.props.heading}</h6>
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

  return BehaviorSwitcherGroup;
});
