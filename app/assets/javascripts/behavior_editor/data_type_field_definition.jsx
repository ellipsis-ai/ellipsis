define(function(require) {
  var React = require('react'),
    DeleteButton = require('./delete_button'),
    FormInput = require('../form/input'),
    Select = require('../form/select'),
    DataTypeField = require('../models/data_type_field'),
    ifPresent = require('../lib/if_present');

  return React.createClass({
    displayName: 'DataTypeFieldDefinition',
    propTypes: {
      id: React.PropTypes.oneOfType([
        React.PropTypes.number,
        React.PropTypes.string
      ]).isRequired,
      field: React.PropTypes.instanceOf(DataTypeField).isRequired,
      paramTypes: React.PropTypes.arrayOf(
        React.PropTypes.shape({
          id: React.PropTypes.string,
          name: React.PropTypes.string
        })
      ).isRequired,
      onChange: React.PropTypes.func.isRequired,
      onDelete: React.PropTypes.func.isRequired,
      shouldGrabFocus: React.PropTypes.bool,
      onConfigureType: React.PropTypes.func.isRequired
    },

    onNameChange: function(newName) {
      this.props.onChange(this.props.field.clone({ name: DataTypeField.formatName(newName) }));
    },

    onTypeChange: function(newTypeId) {
      var newType = this.props.paramTypes.find(ea => ea.id === newTypeId);
      this.props.onChange(this.props.field.clone({ fieldType: newType }));
    },

    onDeleteClick: function() {
      this.props.onDelete();
    },

    onConfigureType: function() {
      this.props.onConfigureType(this.props.field.fieldType.id);
    },

    isConfigurable: function() {
      const pt = this.props.field.fieldType;
      return pt.id !== pt.name;
    },

    focus: function() {
      this.refs.name.focus();
      this.refs.name.select();
    },

    keyFor: function(fieldType) {
      return 'field-type-' + this.props.id + '-' + fieldType.id;
    },

    render: function() {
      return (
        <div>
          <div className="border border-light bg-white plm pbxs">
            <div className="columns columns-elastic">
              <div className="column column-expand align-form-input">
                <FormInput
                  ref="name"
                  className="form-input-borderless type-monospace type-s width-15 mrm"
                  placeholder="dataTypeField"
                  value={this.props.field.name}
                  onChange={this.onNameChange}
                />

                <span className="display-inline-block align-m type-s type-weak mrm mbs">of type</span>
                <Select className="form-select-s form-select-light align-m mrm mbs" name="paramType" value={this.props.field.fieldType.id} onChange={this.onTypeChange}>
                  {this.props.paramTypes.map((fieldType) => (
                    <option value={fieldType.id} key={this.keyFor(fieldType)}>
                      {fieldType.name}
                    </option>
                  ))}
                </Select>
                {ifPresent(this.isConfigurable(), () => (
                  <button type="button" className="button-s button-shrink mbs" onClick={this.onConfigureType}>Edit type…</button>
                ))}
              </div>
              <div className="column column-shrink">
                <DeleteButton
                  onClick={this.onDeleteClick}
                  title={this.props.field.name ? `Delete the “${this.props.field.name}” field` : "Delete this field"}
                />
              </div>
            </div>
          </div>
        </div>
      );
    }
  });

});
