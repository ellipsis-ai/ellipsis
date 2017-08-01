define(function(require) {
  const React = require('react'),
    SectionHeading = require('../shared_ui/section_heading'),
    DataTypeFieldDefinition = require('./data_type_field_definition'),
    Field = require('../models/data_type_field'),
    ParamType = require('../models/param_type'),
    autobind = require('../lib/autobind');

  class DataTypeSchemaConfig extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
      this.fieldComponents = [];
    }

    onChange(index, data) {
      this.props.onChange(index, data);
    }

    onDelete(index) {
      this.props.onDelete(index);
    }

    addField() {
      this.props.onAdd(() => this.focusOnLastField());
    }

    focusOnLastField() {
      const lastFieldIndex = this.props.fields.length - 1;
      if (lastFieldIndex >= 0) {
        this.focusIndex(lastFieldIndex);
      }
    }

    focusOnFirstBlankField() {
      const index = this.props.fields.findIndex((ea) => !ea.name);
      if (index >= 0) {
        this.focusIndex(index);
      }
    }

    focusOnFirstDuplicateField() {
      const dupeIndex = this.props.fields.findIndex((current, index) => current.name && this.props.fields.slice(0, index).some((previous) => previous.name === current.name));
      if (dupeIndex >= 0) {
        this.focusIndex(dupeIndex);
      }
    }

    focusIndex(index) {
      if (this.fieldComponents[index]) {
        this.fieldComponents[index].focus();
      }
    }

    render() {
      return (
        <div className="ptxl">
          <div className="columns container container-narrow">
            <div className="mbxxl">
              <div>
                <SectionHeading number="2">Define fields for the items being stored</SectionHeading>
                <div className="mbm">
                  {this.props.fields.map((field, index) => (
                    <div key={`dataTypeField${index}`} className="mbs">
                      <DataTypeFieldDefinition
                        key={'DataTypeFieldDefinition' + index}
                        ref={(component) => this.fieldComponents[index] = component}
                        field={field}
                        isBuiltIn={index === 0}
                        paramTypes={this.props.paramTypes}
                        onChange={this.onChange.bind(this, index)}
                        onDelete={this.onDelete.bind(this, index)}
                        id={index}
                        onConfigureType={this.props.onConfigureType}
                        behaviorVersionId={this.props.behaviorVersionId}
                      />
                    </div>
                  ))}
                </div>
                <div>
                  <button type="button" className="button-s mrm mbs" onClick={this.addField}>
                    Add another field
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

      );
    }
  }

  DataTypeSchemaConfig.propTypes = {
    onChange: React.PropTypes.func.isRequired,
    onDelete: React.PropTypes.func.isRequired,
    onAdd: React.PropTypes.func.isRequired,
    behaviorVersionId: React.PropTypes.string.isRequired,
    fields: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Field)).isRequired,
    paramTypes: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ParamType)).isRequired,
    animationDisabled: React.PropTypes.bool,
    onConfigureType: React.PropTypes.func.isRequired
  };

  return DataTypeSchemaConfig;
});
