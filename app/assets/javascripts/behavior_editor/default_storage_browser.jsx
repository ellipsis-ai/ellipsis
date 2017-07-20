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

    getTableStatusText(itemRowCount, maxRowCount) {
      if (this.state.isLoading) {
        return "Loading…";
      } else if (itemRowCount === 0) {
        return "No items found";
      } else if (itemRowCount === 1) {
        return "1 item found";
      } else if (itemRowCount <= maxRowCount) {
        return `${itemRowCount} items found`;
      } else {
        return `Showing the first ${maxRowCount} items`;
      }
    }

    renderItem(item, fields, index, isLastItem) {
      return (
        <tr key={`row${index}`}>
          {fields.map((field) => this.renderFieldCell(item, field, isLastItem))}
        </tr>
      );
    }

    renderItems(fields) {
      const items = this.getItems();
      const maxRowCount = DefaultStorageBrowser.getTableRowCount();
      const tableRows = [];
      items.slice(0, maxRowCount).forEach((item, index) => tableRows.push(this.renderItem(item, fields, index, index + 1 === items.length)));
      const itemRowCount = tableRows.length;
      const middleOfRemainingRows = itemRowCount + Math.floor((maxRowCount - itemRowCount) / 2);
      for (let rowIndex = itemRowCount; rowIndex < maxRowCount; rowIndex++) {
        const rowText = rowIndex === middleOfRemainingRows ? this.getTableStatusText(itemRowCount, maxRowCount) : "";
        tableRows.push((
          <tr key={`row${rowIndex}`}>
            <td colSpan={fields.length} className="phxs">
              <div className="align-c type-italic type-weak">{rowText}<br /></div>
            </td>
          </tr>
        ));
      }
      return tableRows;
    }

    renderFieldHeader(field) {
      return (
        <th key={field.fieldId}
          className="bg-lightest border-bottom border-light type-weak phxs"
        >{field.name}</th>
      );
    }

    renderFieldCell(item, field, isLast) {
      const value = item.data[field.name];
      const asString = String(value);
      let className = "";
      if (!value && asString === "null" || asString === "") {
        className = "type-weak";
      }
      return (
        <td key={`${item.id}-${field.fieldId}`}
          className={`phxs ${
            isLast ? "border-bottom border-light" : ""
          }`}
        >
          <span className={className}>{asString || "(empty)"}</span>
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
