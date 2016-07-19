define(function(require) {
  var React = require('react'),
    Input = require('./input'),
    formatEnvVarName = require('./env_var_name_formatter');

  return React.createClass({
    propTypes: {
      index:  React.PropTypes.number.isRequired,
      onCancelClick: React.PropTypes.func.isRequired,
      onSave: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        newVar: {}
      };
    },

    getNewVar: function() {
      return this.state.newVar;
    },

    getName: function() {
      return this.getNewVar().name;
    },

    getValue: function() {
      return this.getNewVar().value;
    },

    hasChanges: function() {
      return this.getValue() && this.getValue().trim().length > 0 && this.getName() && this.getName().trim().length > 0
    },

    focusOnVarName: function() {
      this.refs['envVarName'].focus();
    },

    onCancel: function() {
      this.setState(this.getInitialState());
      this.props.onCancelClick();
    },

    onChangeVarName: function(newName) {
      var newVar = {
        isAlreadySavedWithValue: false,
        name: formatEnvVarName(newName),
        value: this.getValue()
      };
      this.setState({
        newVar: newVar
      });
    },

    onChangeVarValue: function(newValue) {
      var newVar = {
        isAlreadySavedWithValue: false,
        name: this.getName(),
        value: newValue
      };
      this.setState({
        newVar: newVar
      });
    },

    onSave: function() {
      this.props.onSave(this.getNewVar());
    },

    getNameInput: function() {
      return (
        <Input
          ref={"envVarName"}
          className="form-input-left"
          placeholder="Enter name"
          value={this.getName() || ""}
          onChange={this.onChangeVarName}
          />
      );
    },

    getValueInput: function() {
      return (
        <Input
          ref={"envVarValue"}
          className="form-input-right"
          placeholder="Enter value"
          value={this.getValue() || ""}
          onChange={this.onChangeVarValue}
          />
      );
    },

    render: function() {
      return (
        <div className="box-action">
          <div className="container phn">
            <p>
              <span>Add a new environment variable to hold secure information like access keys for other services </span>
              <span>that may be used by multiple behaviors.</span>
            </p>

            <div className="form-grouped-inputs">

              <div className="columns" key="newVar">
                <div className="column column-one-quarter prn">
                  {this.getNameInput()}
                </div>
                <div className="column column-one-quarter pln">
                  {this.getValueInput()}
                </div>
              </div>

            </div>

            <div className="columns mtm">
              <div className="column column-one-half">
                <button type="button"
                        className="button-primary mrs"
                        disabled={!this.hasChanges()}
                        onClick={this.onSave}
                  >Save</button>
                <button type="button" onClick={this.onCancel}>Cancel</button>
              </div>
            </div>

          </div>
        </div>
      );
    }
  });
});
