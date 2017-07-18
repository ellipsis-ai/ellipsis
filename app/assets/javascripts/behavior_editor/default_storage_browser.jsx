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
      if (items.length > 0) {
        return items.map((item) => this.renderItem(item, fields));
      } else {
        return (
          <tr>
            <td colSpan={fields.length} className="phxs">
              <i className="type-weak">No items stored</i>
            </td>
          </tr>
        );
      }
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
