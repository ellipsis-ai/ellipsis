define(function(require) {
  const React = require('react'),
    BehaviorVersion = require('../models/behavior_version'),
    Button = require('../form/button'),
    autobind = require('../lib/autobind');

  class DefaultStorageBrowser extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
    }

    static getTableRowCount() {
      return 20;
    }

    getItems() {
      // TODO
      return [];
    }

    getFields() {
      return this.props.behaviorVersion.getDataTypeConfig().getFields();
    }

    renderItem(item, fields) {
      return (
        <tr key={item.id}>
          {fields.map((field, index) => this.renderFieldCell(item, field, index + 1 < fields.length))}
        </tr>
      );
    }

    renderItems(fields) {
      const items = this.getItems();
      const maxRowCount = DefaultStorageBrowser.getTableRowCount();
      const tableRows = [];
      items.slice(0, maxRowCount).forEach((item) => tableRows.push(this.renderItem(item, fields)));
      const itemRowCount = tableRows.length;
      const middleOfRemainingRows = Math.floor((maxRowCount - itemRowCount) / 2);
      for (let rowIndex = itemRowCount; rowIndex < maxRowCount; rowIndex++) {
        let rowText = "";
        if (rowIndex === middleOfRemainingRows) {
          rowText = itemRowCount === 0 ? "No items stored" : "End of items";
        }
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
      return (
        <td key={`${item.id}-${field.fieldId}`}
          className={`phxs ${
            isLast ? "border-bottom border-light" : ""
          }`}
        >{item.data[field.name]}</td>
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

                <table className="type-s border border-light">
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
                    className="mrs mbs"
                    onClick={this.props.onCancelClick}
                  >Done</Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
  }

  DefaultStorageBrowser.propTypes = {
    behaviorVersion: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
    onCancelClick: React.PropTypes.func.isRequired
  };

  return DefaultStorageBrowser;
});
