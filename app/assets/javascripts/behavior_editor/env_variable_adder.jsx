define(function(require) {
  var React = require('react'),
    Input = require('./input'),
    Textarea = require('./textarea'),
    formatEnvVarName = require('./env_var_name_formatter');

  return React.createClass({
    propTypes: {
      prompt: React.PropTypes.string,
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

    hasNameAndValue: function() {
      return this.getValue() && this.getValue().trim().length > 0 && this.getName() && this.getName().trim().length > 0;
    },

    focusOnVarName: function() {
      this.refs['envVarName'].focus();
    },

    onCancel: function() {
      this.reset();
      this.props.onCancelClick();
    },

    onChangeVarName: function(newName) {
      var newVar = {
        isAlreadySavedWithName: false,
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
        isAlreadySavedWithName: false,
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

    reset: function() {
      this.setState(this.getInitialState());
    },

    getPrompt: function() {
      if (this.props.prompt) {
        return (
          <p>{this.props.prompt}</p>
        );
      } else {
        return (
          <div>
            <h4>Add a new environment variable</h4>
            <p className="type-weak type-s">Environment variables can hold secure information like access keys for other
              services.</p>
          </div>
        );
      }
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
        <Textarea
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
            {this.getPrompt()}

            <div className="form-grouped-inputs">

              <div className="columns" key="newVar">
                <div className="column column-one-quarter mobile-column-one-half prn">
                  {this.getNameInput()}
                </div>
                <div className="column column-one-quarter mobile-column-one-half pln">
                  {this.getValueInput()}
                </div>
              </div>

            </div>

            <div className="mtm">
              <button type="button"
                      className="button-primary mrs mbs"
                      disabled={!this.hasNameAndValue()}
                      onClick={this.onSave}
                >Save</button>
              <button className="mbs" type="button" onClick={this.onCancel}>Cancel</button>
            </div>

          </div>
        </div>
      );
    }
  });
});
