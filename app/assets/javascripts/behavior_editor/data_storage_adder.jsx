define(function(require) {
  const React = require('react'),
    BehaviorVersion = require('../models/behavior_version'),
    Collapsible = require('../shared_ui/collapsible'),
    DataStorageAdderField = require('./data_storage_adder_field'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils');

  class DataStorageAdder extends React.Component {
    constructor(props) {
      super(props);
      this.state = {
        fieldValues: this.getBlankValuesFor(props.behaviorVersion),
        lastSavedItem: null,
        isSaving: false,
        error: null
      };
      this.inputs = [];
      this.saveButton = null;
      ['save', 'cancel', 'onEnterKey'].forEach((ea) => this[ea] = this[ea].bind(this));
    }

    componentWillReceiveProps(newProps) {
      if (newProps.behaviorVersion.id !== this.props.behaviorVersion.id ||
          newProps.behaviorVersion.getDataTypeFields() !== this.props.behaviorVersion.getDataTypeFields()) {
        this.setState({
          fieldValues: this.getBlankValuesFor(newProps.behaviorVersion),
          lastSavedItem: null,
          error: null
        });
      }
    }

    getBlankValuesFor(behaviorVersion) {
      return this.getWritableFieldsFor(behaviorVersion).map(() => "");
    }

    getAllFieldsFor(behaviorVersion) {
      return behaviorVersion.getDataTypeFields();
    }

    getWritableFieldsFor(behaviorVersion) {
      return this.getAllFieldsFor(behaviorVersion).slice(1);
    }

    updateFieldValue(index, newValue) {
      this.setState({
        fieldValues: ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.fieldValues, newValue, index)
      });
    }

    hasValues() {
      return this.state.fieldValues.some((ea) => ea.length > 0);
    }

    hasSavedItem() {
      return Boolean(this.state.lastSavedItem);
    }

    save() {
      this.setState({
        error: null,
        isSaving: true
      }, () => {
        const newItem = {};
        this.getWritableFieldsFor(this.props.behaviorVersion).forEach((field, index) => {
          newItem[field.fieldId] = this.state.fieldValues[index];
        });

        // TODO: Hook this up to an actual save mechanism
        
        this.setState({
          lastSavedItem: newItem,
          isSaving: false,
          fieldValues: this.getBlankValuesFor(this.props.behaviorVersion)
        }, () => {
          if (this.inputs[0]) {
            this.inputs[0].focus();
          }
        });
      });
    }

    isSaving() {
      return this.state.isSaving;
    }

    cancel() {
      this.props.onCancelClick();
    }

    onEnterKey(index) {
      if (this.inputs[index + 1]) {
        this.inputs[index + 1].focus();
      } else if (this.saveButton) {
        this.saveButton.focus();
      }
    }

    render() {
      this.inputs = [];
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak">Add data to {this.props.behaviorVersion.getName() || "this data type"}</h4>
              </div>
              <div className="column column-page-main">
                <Collapsible revealWhen={this.hasSavedItem()}>
                  <h4>Last saved item</h4>
                  {this.state.lastSavedItem ? (
                    <div className={
                      `columns columns-elastic bg-lightest border border-light phm type-weak mbxl ${
                        this.isSaving() ? "pulse" : ""
                      }`
                    }>
                      <div className="column-group">
                        {this.getAllFieldsFor(this.props.behaviorVersion).map((field) => (
                          <DataStorageAdderField
                            key={`lastSaved-${field.fieldId}`}
                            field={field}
                            value={this.state.lastSavedItem[field.fieldId] || ""}
                            readOnly={true}
                          />
                        ))}
                      </div>
                    </div>
                  ) : null}
                </Collapsible>

                <h4>New item</h4>

                <div className="columns columns-elastic border border-light phm">
                  <div className="column-group">
                    {this.getWritableFieldsFor(this.props.behaviorVersion).map((field, index) => (
                      <DataStorageAdderField
                        key={`nextItem-${field.fieldId}`}
                        ref={(input) => this.inputs[index] = input}
                        field={field}
                        value={this.state.fieldValues[index]}
                        onChange={this.updateFieldValue.bind(this, index)}
                        onEnterKey={this.onEnterKey.bind(this, index)}
                      />
                    ))}
                  </div>
                </div>

                <div className="ptxl">
                  <DynamicLabelButton className="button-primary mrs mbs"
                    ref={(button) => this.saveButton = button}
                    onClick={this.save}
                    disabledWhen={!this.hasValues() || this.isSaving()}
                    labels={[{
                      text: "Save",
                      displayWhen: !this.isSaving()
                    }, {
                      text: "Saving…",
                      displayWhen: this.isSaving()
                    }]}
                   />
                  <DynamicLabelButton className="mrs mbs"
                    onClick={this.cancel}
                    disabledWhen={this.isSaving()}
                    labels={[{
                      text: "Done",
                      displayWhen: !this.hasValues()
                    }, {
                      text: "Cancel",
                      displayWhen: this.hasValues()
                    }]}
                  />
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
    onCancelClick: React.PropTypes.func.isRequired
  };

  return DataStorageAdder;
});
