define(function(require) {
  const React = require('react'),
    BehaviorVersion = require('../models/behavior_version'),
    Button = require('../form/button'),
    DataStorageAdderField = require('./data_storage_adder_field'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils');

  class DataStorageAdder extends React.Component {
    constructor(props) {
      super(props);
      this.state = {
        fieldValues: this.getBlankValuesFor(props.behaviorVersion)
      };
      ['save', 'cancel'].forEach((ea) => this[ea] = this[ea].bind(this));
    }

    componentWillReceiveProps(newProps) {
      if (newProps.behaviorVersion !== this.props.behaviorVersion) {
        this.setState({
          fieldValues: this.getBlankValuesFor(newProps.behaviorVersion)
        });
      }
    }

    getBlankValuesFor(behaviorVersion) {
      return this.getWritableFieldsFor(behaviorVersion).map(() => "");
    }

    getWritableFieldsFor(behaviorVersion) {
      return behaviorVersion.getDataTypeFields().filter((ea) => !ea.isGenerated());
    }

    updateFieldValue(index, newValue) {
      this.setState({
        fieldValues: ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.fieldValues, newValue, index)
      });
    }

    hasValues() {
      return this.state.fieldValues.some((ea) => ea.length > 0);
    }

    save() {
      const newItem = {};
      this.getWritableFieldsFor(this.props.behaviorVersion).forEach((field, index) => {
        newItem[field.fieldId] = this.state.fieldValues[index];
      });
      this.props.onSave(newItem);
      this.setState({
        fieldValues: this.getBlankValuesFor(this.props.behaviorVersion)
      });
    }

    cancel() {
      this.props.onCancelClick();
    }

    render() {
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak">Add data to {this.props.behaviorVersion.getName() || "this data type"}</h4>
              </div>
              <div className="column column-page-main">
                <h4>New item</h4>

                <div className="columns columns-elastic">
                  <div className="column-group">
                    {this.getWritableFieldsFor(this.props.behaviorVersion).map((field, index) => (
                      <DataStorageAdderField
                        key={field.fieldId}
                        field={field}
                        value={this.state.fieldValues[index]}
                        onChange={this.updateFieldValue.bind(this, index)}
                      />
                    ))}
                  </div>
                </div>

                <div className="ptxl">
                  <Button className="button-primary mrs mbs" onClick={this.save} disabled={!this.hasValues()}>Save
                    item</Button>
                  <Button className="mrs mbs" onClick={this.cancel}>Cancel</Button>
                </div>

              </div>
            </div>
          </div>
        </div>
      );
    }
  }

  DataStorageAdder.propTypes = {
    behaviorVersion: React.PropTypes.instanceOf(BehaviorVersion).isRequired,
    onSave: React.PropTypes.func.isRequired,
    onCancelClick: React.PropTypes.func.isRequired
  };

  return DataStorageAdder;
});
