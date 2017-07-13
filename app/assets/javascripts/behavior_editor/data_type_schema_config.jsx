define(function(require) {
  var React = require('react'),
    SectionHeading = require('../shared_ui/section_heading'),
    DataTypeFieldDefinition = require('./data_type_field_definition'),
    Field = require('../models/data_type_field'),
    ParamType = require('../models/param_type');

  return React.createClass({
    displayName: 'DataTypeSchemaConfig',
    propTypes: {
      onChange: React.PropTypes.func.isRequired,
      onDelete: React.PropTypes.func.isRequired,
      onAdd: React.PropTypes.func.isRequired,
      behaviorVersionId: React.PropTypes.string.isRequired,
      fields: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Field)).isRequired,
      paramTypes: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ParamType)).isRequired,
      animationDisabled: React.PropTypes.bool,
      onConfigureType: React.PropTypes.func.isRequired
    },

    onChange: function(index, data) {
      this.props.onChange(index, data);
    },
    onDelete: function(index) {
      this.props.onDelete(index);
    },

    addField: function() {
      this.props.onAdd(() => this.focusOnLastField());
    },

    fieldComponents: [],

    focusOnLastField: function() {
      const lastFieldIndex = this.props.fields.length - 1;
      if (lastFieldIndex >= 0) {
        this.focusIndex(lastFieldIndex);
      }
    },

    focusOnFirstBlankField: function() {
      const index = this.props.fields.findIndex((ea) => !ea.name);
      if (index >= 0) {
        this.focusIndex(index);
      }
    },

    focusIndex: function(index) {
      if (this.fieldComponents[index]) {
        this.fieldComponents[index].focus();
      }
    },

    render: function() {
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
  });
});
