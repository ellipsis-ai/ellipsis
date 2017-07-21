define(function(require) {
  const React = require('react'),
    BehaviorVersion = require('../models/behavior_version'),
    Button = require('../form/button'),
    DataRequest = require('../lib/data_request'),
    DefaultStorageItem = require('../models/default_storage_item'),
    autobind = require('../lib/autobind');

  class DefaultStorageBrowser extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        items: [],
        isLoading: false,
        error: null
      };
    }

    componentWillReceiveProps(newProps) {
      if (!this.props.isVisible && newProps.isVisible) {
        this.loadItems();
      }
    }

    static getTableRowCount() {
      return 20;
    }

    getGraphQLQuery() {
      try {
        return this.props.behaviorVersion.buildGraphQLListQuery();
      } catch(err) {
        this.onErrorLoadingData();
      }
    }

    loadItems() {
      this.setState({
        items: [],
        isLoading: true,
        error: null
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

    onLoadedData(json) {
      const queryName = this.props.behaviorVersion.getGraphQLListQueryName();
      try {
        const items = json.data[queryName];
        this.setState({
          isLoading: false,
          items: items.map((ea) => new DefaultStorageItem({ data: ea }))
        });
      } catch(err) {
        this.onErrorLoadingData();
      }
    }

    onErrorLoadingData() {
      this.setState({
        isLoading: false,
        error: "An error occurred while loading the data."
      });
    }

    getItems() {
      return this.state.items;
    }

    getFields() {
      return this.props.behaviorVersion.getDataTypeConfig().getFields();
    }

    getTableStatusText(itemCount, maxItemCount) {
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

    renderItem(item, fields, index, isLastItem) {
      return (
        <tr key={`row${index}`}>
          {fields.map((field) => this.renderFieldCell(item, field, index, isLastItem))}
        </tr>
      );
    }

    renderItems(fields) {
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
              colSpan={fields.length}
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

    renderFieldHeader(field) {
      return (
        <th key={field.fieldId}
          className="bg-light border-bottom border-light type-monospace type-weak phxs"
        >{field.name}</th>
      );
    }

    isEmptyValue(value) {
      return value === null || value === undefined || value === "";
    }

    renderValue(value) {
      const className = this.isEmptyValue(value) ? "type-disabled" : "";
      const asString = String(value) || "(empty)";
      return (
        <span className={className}>{asString}</span>
      );
    }

    renderFieldCell(item, field, index, isLast) {
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

    render() {
      const fields = this.getFields();
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak">Browse data stored
                  with {this.props.behaviorVersion.getName() || "this data type"}</h4>
              </div>
              <div className="column column-page-main">

                <table className={`type-s border border-light ${
                  this.state.isLoading ? "pulse" : ""
                }`}>
                  <thead>
                    <tr>
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

  DefaultStorageBrowser.propTypes = {
    csrfToken: React.PropTypes.string.isRequired,
    behaviorVersion: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
    behaviorGroupId: React.PropTypes.string.isRequired,
    onCancelClick: React.PropTypes.func.isRequired,
    isVisible: React.PropTypes.bool.isRequired
  };

  return DefaultStorageBrowser;
});
