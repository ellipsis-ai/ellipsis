define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    SetOps = require('../lib/set_operations');

  return React.createClass({
    displayName: 'ChangeSummary',
    propTypes: {
      currentGroupVersion: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      originalGroupVersion: React.PropTypes.instanceOf(BehaviorGroup).isRequired,
      isModified: React.PropTypes.func.isRequired
    },

    getResultFor: function(actionCount, dataTypeCount, adjective) {
      let result = "";
      if (actionCount > 1) {
        if (dataTypeCount > 1) {
          result = `${actionCount} actions and ${dataTypeCount} data types ${adjective}`;
        } else if (dataTypeCount === 1) {
          result = `${actionCount} actions and 1 data type ${adjective}`;
        } else {
          result = `${actionCount} actions ${adjective}`;
        }
      } else if (actionCount === 1) {
        if (dataTypeCount > 1) {
          result = `1 action and ${dataTypeCount} data types ${adjective}`;
        } else if (dataTypeCount === 1) {
          result = `1 action and 1 data type ${adjective}`;
        } else {
          result = `1 action ${adjective}`;
        }
      } else {
        if (dataTypeCount > 1) {
          result = `${dataTypeCount} data types ${adjective}`;
        } else if (dataTypeCount === 1) {
          result = `1 data type ${adjective}`;
        }
      }
      return result;
    },

    render: function() {
      const currentActions = this.props.currentGroupVersion.getActions();
      const currentDataTypes = this.props.currentGroupVersion.getDataTypes();
      const originalActions = this.props.originalGroupVersion.getActions();
      const originalDataTypes = this.props.originalGroupVersion.getDataTypes();

      const actionsModified = currentActions.filter((ea) => this.props.isModified(ea) && !ea.isNewBehavior).length;
      const dataTypesModified = currentDataTypes.filter((ea) => this.props.isModified(ea) && !ea.isNewBehavior).length;

      const currentActionIds = currentActions.map((ea) => ea.id);
      const originalActionIds = originalActions.map((ea) => ea.id);

      const actionsAdded = SetOps.difference(currentActionIds, originalActionIds).length;
      const actionsRemoved = SetOps.difference(originalActionIds, currentActionIds).length;

      const currentDataTypeIds = currentDataTypes.map((ea) => ea.id);
      const originalDataTypeIds = originalDataTypes.map((ea) => ea.id);

      const dataTypesAdded = SetOps.difference(currentDataTypeIds, originalDataTypeIds).length;
      const dataTypesRemoved = SetOps.difference(originalDataTypeIds, currentDataTypeIds).length;

      const addedResult = this.getResultFor(actionsAdded, dataTypesAdded, "added");
      const removedResult = this.getResultFor(actionsRemoved, dataTypesRemoved, "removed");
      const modifiedResult = this.getResultFor(actionsModified, dataTypesModified, "modified");

      const result = [addedResult, removedResult, modifiedResult].filter((ea) => ea.length > 0).join("; ");

      return (
        <span className="fade-in type-pink type-italic">
          <span className="type-bold">Unsaved changes </span>
          <span>{result ? `(${result})` : ""}</span>
        </span>
      );
    }
  });
});
