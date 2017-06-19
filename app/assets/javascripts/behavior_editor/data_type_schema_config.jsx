define(function(require) {
  var React = require('react'),
    SectionHeading = require('./section_heading'),
    DataTypeFieldDefinition = require('./data_type_field_definition'),
    Field = require('../models/data_type_field');

  return React.createClass({
    displayName: 'DataTypeSchemaConfig',
    propTypes: {
      onChange: React.PropTypes.func.isRequired,
      onDelete: React.PropTypes.func.isRequired,
      onAdd: React.PropTypes.func.isRequired,
      fields: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Field)).isRequired,
      paramTypes: React.PropTypes.arrayOf(
        React.PropTypes.shape({
          id: React.PropTypes.string.isRequired,
          name: React.PropTypes.string.isRequired
        })
      ).isRequired,
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
      this.props.onAdd();
    },

    focusIndex: function(index) {
      this.refs['field' + index].focus();
    },

    hasFields: function() {
      return this.props.fields.length > 0;
    },

    render: function() {
      return (
        <div>


          <hr className="mtn thin bg-gray-light" />

          <div className="columns container container-narrow">
            <div className="mbxxl">
              <div>
                <SectionHeading number="1">Define the fields</SectionHeading>
                <div className="mbm">
                  {this.props.fields.map((field, index) => (
                    <div key={`dataTypeField${index}`}>
                      <DataTypeFieldDefinition
                        key={'DataTypeFieldDefinition' + index}
                        ref={'field' + index}
                        field={field}
                        paramTypes={this.props.paramTypes}
                        onChange={this.onChange.bind(this, index)}
                        onDelete={this.onDelete.bind(this, index)}
                        id={index}
                        onConfigureType={this.props.onConfigureType}
                      />
                      {index + 1 < this.props.fields.length ? (
                        <div className="pvxs type-label type-disabled align-c">and</div>
                      ) : null}
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
