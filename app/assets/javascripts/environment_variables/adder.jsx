define(function(require) {
  var React = require('react'),
    Input = require('../form/input'),
    Textarea = require('../form/textarea'),
    formatEnvVarName = require('./formatter'),
    ifPresent = require('../if_present');

  return React.createClass({
    propTypes: {
      prompt: React.PropTypes.string,
      onCancelClick: React.PropTypes.func.isRequired,
      onSave: React.PropTypes.func.isRequired,
      existingNames: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
    },

    getInitialState: function() {
      return {
        newVar: {},
        saveError: false,
        isSaving: false
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

    nameIsDuplicate: function() {
      var newName = this.getName();
      return newName && this.props.existingNames.some((ea) => ea === newName);
    },

    isValid: function() {
      return this.hasNameAndValue() && !this.nameIsDuplicate();
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
      this.setState({
        saveError: false,
        isSaving: true
      }, () => {
        this.props.onSave(this.getNewVar());
      });
    },

    onSaveError: function() {
      this.setState({
        saveError: true,
        isSaving: false
      });
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
                className={"button-primary mrs mbs " + (this.state.isSaving ? "button-activated" : "")}
                disabled={!this.isValid()}
                onClick={this.onSave}
              >
                <span className="button-labels">
                  <span className="button-normal-label">Save</span>
                  <span className="button-activated-label">Saving…</span>
                </span>
              </button>
              <button className="mbs mrl" type="button" onClick={this.onCancel}>Cancel</button>
              {ifPresent(this.state.saveError, () => (
                <span className="mbs type-pink type-bold align-button fade-in">
                  An error occurred while saving. Please try again.
                </span>
              ))}
              {ifPresent(this.nameIsDuplicate(), () => (
                <span className="mbs type-pink align-button fade-in">
                  There is already a variable named {this.getName()}.
                </span>
              ))}
            </div>

          </div>
        </div>
      );
    }
  });
});
