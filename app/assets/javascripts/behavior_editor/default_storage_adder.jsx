// @flow
define(function(require) {
  const React = require('react'),
    BehaviorVersion = require('../models/behavior_version'),
    Collapsible = require('../shared_ui/collapsible'),
    DefaultStorageAdderField = require('./default_storage_adder_field'),
    Button = require('../form/button'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    DataRequest = require('../lib/data_request'),
    DataTypeField = require('../models/data_type_field'),
    DefaultStorageItem = require('../models/default_storage_item'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils'),
    autobind = require('../lib/autobind');

  type Props = {
    csrfToken: string,
    behaviorVersion: BehaviorVersion,
    onCancelClick: () => void
  }

  type State = {
    fieldValues: Array<any>,
    lastSavedItem: DefaultStorageItem,
    isSaving: boolean,
    error: ?string
  }

  class DefaultStorageAdder extends React.Component<Props, State> {
    props: Props;
    state: State;

    constructor(props: Props) {
      super(props);
      this.state = {
        fieldValues: this.getDefaultValuesFor(props.behaviorVersion),
        lastSavedItem: new DefaultStorageItem(),
        isSaving: false,
        error: null
      };
      this.inputs = [];
      this.saveButton = null;
      autobind(this);
    }

    componentWillReceiveProps(newProps: Props) {
      if (newProps.behaviorVersion.id !== this.props.behaviorVersion.id ||
          newProps.behaviorVersion.getDataTypeFields() !== this.props.behaviorVersion.getDataTypeFields()) {
        this.setState({
          fieldValues: this.getDefaultValuesFor(newProps.behaviorVersion),
          lastSavedItem: new DefaultStorageItem(),
          error: null
        });
      }
    }

    getDefaultValuesFor(behaviorVersion): Array<any> {
      return this.getWritableFieldsFor(behaviorVersion).map((field) => field.fieldType.getDefaultValue());
    }

    getWritableFieldsFor(behaviorVersion): Array<DataTypeField> {
      return behaviorVersion.getWritableDataTypeFields();
    }

    getLastSavedItemFields(): Array<DataTypeField> {
      return this.state.lastSavedItem.fields;
    }

    updateFieldValue(index: number, newValue: any): void {
      this.setState({
        fieldValues: ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.fieldValues, newValue, index)
      });
    }

    hasValues(): boolean {
      return this.state.fieldValues.some((ea) => ea.length > 0);
    }

    hasSavedItem(): boolean {
      return this.state.lastSavedItem.fields.length > 0;
    }

    save(): void {
      this.setState({
        error: null,
        isSaving: true
      }, () => {
        const newItem = {};
        this.getWritableFieldsFor(this.props.behaviorVersion).forEach((field, index) => {
          newItem[field.name] = this.state.fieldValues[index];
        });
        const url = jsRoutes.controllers.BehaviorEditorController.saveDefaultStorageItem().url;

        DataRequest.jsonPost(url, {
          itemJson: JSON.stringify({
            behaviorId: this.props.behaviorVersion.behaviorId,
            data: newItem
          })
        }, this.props.csrfToken)
          .then((savedItem) => {
            if (savedItem.data) {
              this.onSavedNewItem(savedItem);
            } else {
              throw new Error();
            }
          })
          .catch(this.onErrorSaving);
      });
    }

    onSavedNewItem(savedItemData: { [string]: any }): void {
      this.setState({
        lastSavedItem: new DefaultStorageItem(
          savedItemData.id,
          this.props.behaviorVersion.behaviorId,
          savedItemData.updatedAt,
          savedItemData.updatedByUserId,
          savedItemData.data
        ),
        isSaving: false,
        fieldValues: this.getDefaultValuesFor(this.props.behaviorVersion)
      }, this.focusFirstInput);
    }

    onErrorSaving(): void {
      this.setState({
        isSaving: false,
        error: "An error occurred while saving. Please try again."
      });
    }

    isSaving(): boolean {
      return this.state.isSaving;
    }

    cancel(): void {
      this.props.onCancelClick();
    }

    onEnterKey(index: number): void {
      if (this.inputs[index + 1]) {
        this.inputs[index + 1].focus();
      } else if (this.saveButton) {
        this.saveButton.focus();
      }
    }

    focusFirstInput(): void {
      if (this.inputs[0]) {
        this.inputs[0].focus();
      }
    }

    renderError(): React.Node {
      return this.state.error ? (
        <div className="align-button mbs fade-in type-pink type-bold type-italic">{this.state.error}</div>
      ) : null;
    }

    renderLastSavedItem(): React.Node {
      return this.getLastSavedItemFields().map((field) => (
        <DefaultStorageAdderField
          key={`lastSaved-${field.name}`}
          name={field.name}
          value={field.stringValue}
          readOnly={true}
        />
      ));
    }

    render(): React.Node {
      this.inputs = [];
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="mtn type-weak">Add data to {this.props.behaviorVersion.getName() || "this data type"}</h4>
              </div>
              <div className="column column-page-main">
                <Collapsible revealWhen={this.hasSavedItem()}>
                  <h4>Last saved item</h4>
                  <div className={`bg-lightest border border-light phm type-weak mbxl ${
                    this.isSaving() ? "pulse" : ""
                  }`}>
                    <div className="columns columns-elastic">
                      <div className="column-group">
                        {this.renderLastSavedItem()}
                      </div>
                    </div>
                  </div>
                </Collapsible>

                <h4>New item</h4>

                <div className="border border-light phm">
                  <div className="columns columns-elastic">
                    <div className="column-group">
                      {this.getWritableFieldsFor(this.props.behaviorVersion).map((field, index) => (
                        <DefaultStorageAdderField
                          key={`nextItem-${field.fieldId}`}
                          ref={(input) => this.inputs[index] = input}
                          name={field.name}
                          value={this.state.fieldValues[index]}
                          onChange={this.updateFieldValue.bind(this, index)}
                          onEnterKey={this.onEnterKey.bind(this, index)}
                          fieldType={field.fieldType}
                        />
                      ))}
                    </div>
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
                  <Button className="mrxl mbs"
                    onClick={this.cancel}
                    disabled={this.isSaving()}
                  >Done</Button>
                  {this.renderError()}
                </div>

              </div>
            </div>
          </div>
        </div>
      );
    }
  }

  return DefaultStorageAdder;
});
