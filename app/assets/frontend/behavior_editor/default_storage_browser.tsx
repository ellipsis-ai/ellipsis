import * as React from 'react';
import BehaviorVersion from '../models/behavior_version';
import Button from '../form/button';
import Checkbox from '../form/checkbox';
import {DataRequest} from '../lib/data_request';
import DataTypeField from '../models/data_type_field';
import DefaultStorageItem from '../models/default_storage_item';
import autobind from '../lib/autobind';

type Props = {
  csrfToken: string,
  behaviorVersion: BehaviorVersion,
  behaviorGroupId: string,
  onCancelClick: () => void,
  isVisible: boolean
}

type State = {
  items: Array<DefaultStorageItem>,
  isLoading: boolean,
  error: string | null,
  checkedIds: Array<string>
}

class DefaultStorageBrowser extends React.Component<Props, State> {
    props: Props;
    state: State;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        items: [],
        isLoading: false,
        error: null,
        checkedIds: []
      };
    }

    componentWillReceiveProps(newProps: Props) {
      if (!this.props.isVisible && newProps.isVisible) {
        this.loadItems();
      }
    }

    static getTableRowCount(): number {
      return 20;
    }

    getGraphQLQuery(): string | void {
      try {
        return this.props.behaviorVersion.buildGraphQLListQuery();
      } catch(err) {
        this.onErrorLoadingData();
      }
    }

    loadItems(): void {
      this.setState({
        items: [],
        isLoading: true,
        error: null,
        checkedIds: []
      }, () => {
        const url = jsRoutes.controllers.BehaviorEditorController.queryDefaultStorage().url;
        DataRequest.jsonPost(url, {
            behaviorGroupId: this.props.behaviorGroupId,
            query: this.getGraphQLQuery(),
            operationName: null,
            variables: null
          }, this.props.csrfToken)
          .then(this.onLoadedData)
          .catch(this.onErrorLoadingData);
      });
    }

    onLoadedData(json: { [prop: string]: any }): void {
      const queryName = this.props.behaviorVersion.getGraphQLListQueryName();
      try {
        const items = json.data[queryName].map((data) => {
          return new DefaultStorageItem(
            data.id,
            this.props.behaviorVersion.behaviorId,
            data.updatedAt,
            data.updatedByUserId,
            data
          );
        });
        this.setState({
          isLoading: false,
          items: items
        });
      } catch(err) {
        this.onErrorLoadingData();
      }
    }

    onErrorLoadingData(): void {
      this.setState({
        isLoading: false,
        error: "An error occurred while loading the data."
      });
    }

    getItems(): Array<DefaultStorageItem> {
      return this.state.items;
    }

    getFields(): Array<DataTypeField> {
      return this.props.behaviorVersion.getDataTypeConfig().getFields();
    }

    getTableStatusText(itemCount: number, maxItemCount: number): string {
      if (this.state.isLoading) {
        return "Loading…";
      } else if (itemCount === 0) {
        return "No items found";
      } else if (itemCount === 1) {
        return "1 item found";
      } else if (itemCount <= maxItemCount) {
        return `${itemCount} items found`;
      } else {
        return `Showing the first ${maxItemCount} items out of ${itemCount}`;
      }
    }

    renderItem(item: DefaultStorageItem, fields: Array<DataTypeField>, index: number, isLastItem: boolean) {
      return (
        <tr key={`row${index}`}>
          {this.renderCheckboxCell(item)}
          {fields.map((field) => this.renderFieldCell(item, field, index, isLastItem))}
        </tr>
      );
    }

    renderItems(fields: Array<DataTypeField>) {
      const items = this.getItems();
      const maxItemCount = DefaultStorageBrowser.getTableRowCount();
      const maxRowCount = maxItemCount + 1;
      const tableRows = items.slice(0, maxItemCount).map((item, index) => {
        return this.renderItem(item, fields, index, index + 1 === items.length);
      });
      const itemRowCount = tableRows.length;
      const middleOfRemainingRows = itemRowCount + Math.ceil((maxRowCount - itemRowCount) / 2);
      for (let rowIndex = itemRowCount; rowIndex < maxRowCount; rowIndex++) {
        const rowText = rowIndex + 1 === middleOfRemainingRows ? this.getTableStatusText(items.length, maxItemCount) : "";
        tableRows.push((
          <tr key={`row${rowIndex}`}>
            <td
              colSpan={fields.length+1}
              className={`phxs bg-light align-c type-italic type-weak ${
                rowIndex + 1 === maxRowCount ? "border-bottom border-light" : ""
              }`}>
              <div>{rowText}<br /></div>
            </td>
          </tr>
        ));
      }
      return tableRows;
    }

    renderFieldHeader(field: DataTypeField, index: number) {
      return (
        <th key={`field${index}`}
          className="bg-light border-bottom border-light type-monospace type-weak phxs"
        >{field.name}</th>
      );
    }

    isEmptyValue(value: any): boolean {
      return value === null || value === undefined || value === "";
    }

    renderValue(value: any) {
      const className = this.isEmptyValue(value) ? "type-disabled" : "";
      const asString = String(value) || "(empty)";
      return (
        <span className={className}>{asString}</span>
      );
    }

    getCheckedIds(): Array<string> {
      return this.state.checkedIds || [];
    }

    checkedItemsCount(): number {
      return this.getCheckedIds().length;
    }

    onDeleteItemsClick(): void {
      const url = jsRoutes.controllers.BehaviorEditorController.deleteDefaultStorageItems().url;

      DataRequest.jsonPost(url, {
        behaviorId: this.props.behaviorVersion.behaviorId,
        itemIds: this.getCheckedIds()
      }, this.props.csrfToken)
        .then(() => {
          this.loadItems();
        })
        .catch(this.onErrorDeleting);
    }

    onErrorDeleting(): void {
      this.setState({
        error: "An error occurred while deleting. Please try again."
      });
    }

  isChecked(item: DefaultStorageItem): boolean {
      return this.getCheckedIds().indexOf(item.data.id) >= 0;
    }

    toggleChecked(checked: boolean, itemId: string): void {
      let checkedIdsAfter = this.getCheckedIds();
      if (checked) {
        checkedIdsAfter = checkedIdsAfter.concat([itemId]);
      } else {
        const idx = this.getCheckedIds().indexOf(itemId);
        checkedIdsAfter.splice(idx, 1);
      }
      this.setState({
        checkedIds: checkedIdsAfter
      });
    }

    renderCheckboxCell(item: DefaultStorageItem) {
      const itemId = item.data.id;
      return (
        <td
          key={`${itemId}-checkbox`}
          className="phm"
        >
          <Checkbox
            checked={this.isChecked(item)}
            onChange={this.toggleChecked}
            value={itemId}
            >
          </Checkbox>
        </td>
      );
    }

    renderFieldCell(item: DefaultStorageItem, field: DataTypeField, index: number, isLast: boolean) {
      const value = item.data[field.name];
      const className = `phxs type-monospace border-light border-right ${
        isLast ? "border-bottom " : ""
      } ${
        index % 2 === 1 ? "bg-lightest" : ""
      }`;
      return (
        <td
          key={`${item.id}-${field.fieldId}`}
          className={className}
        >
          {this.renderValue(value)}
        </td>
      );
    }

    getDeleteButtonText(): string {
      const count = this.checkedItemsCount();
      if (count === 0) {
        return "Delete items";
      } else if (count === 1) {
        return "Delete one item";
      } else {
        return `Delete ${this.checkedItemsCount()} items`;
      }
    }

    render() {
      const fields = this.getFields();
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="mtn type-weak">Browse data stored
                  with {this.props.behaviorVersion.getName() || "this data type"}</h4>
              </div>
              <div className="column column-page-main">

                <table className={`type-s border border-light ${
                  this.state.isLoading ? "pulse" : ""
                }`}>
                  <thead>
                    <tr>
                      <th className="bg-light border-bottom border-light type-monospace type-weak phxs" />
                      {fields.map(this.renderFieldHeader)}
                    </tr>
                  </thead>
                  <tbody>
                    {this.renderItems(fields)}
                  </tbody>
                </table>

                <div className="ptxl">

                  <Button
                    className="mrl mbs"
                    onClick={this.props.onCancelClick}
                  >Done</Button>
                  <Button
                    disabled={this.checkedItemsCount() === 0}
                    className="mrl mbs"
                    onClick={this.onDeleteItemsClick}
                  >{this.getDeleteButtonText()}</Button>
                  {this.state.error ? (
                    <span className="align-button mbs type-bold type-pink type-italic fade-in">{this.state.error}</span>
                  ) : null}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
}

export default DefaultStorageBrowser;

