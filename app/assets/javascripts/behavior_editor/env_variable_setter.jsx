define(function(require) {
  var React = require('react'),
    Input = require('./input'),
    ImmutableObjectUtils = require('../immutable_object_utils'),
    formatEnvVarName = require('./env_var_name_formatter');

  return React.createClass({
    propTypes: {
      onCancelClick: React.PropTypes.func.isRequired,
      onChangeVarName: React.PropTypes.func.isRequired,
      onSave: React.PropTypes.func.isRequired,
      vars: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    getVars: function() {
      return this.state.vars;
    },

    getInitialState: function() {
      return {
        vars: this.props.vars
      };
    },

    componentWillReceiveProps: function(newProps) {
      if (newProps.vars.length !== this.state.vars.length) {
        this.setState(this.getInitialState());
      }
    },

    hasNameAndValue: function() {
      return !this.getVars().every(function(v, index) {
        var theVar = this.props.vars[index];
        return theVar &&
          v.name === theVar.name &&
          v.value === theVar.value &&
          v.isAlreadySavedWithName === theVar.isAlreadySavedWithName &&
          v.isAlreadySavedWithValue === theVar.isAlreadySavedWithValue;
      }, this);
    },

    focusOnVarName: function(name) {
      var numVars = this.getVars().length;
      var varFound = false;
      for (var i = 0; i < numVars && !varFound; i++) {
        if (this.getVars()[i].name === name) {
          this.refs['envVarValue' + i].focus();
          varFound = true;
        }
      }
    },

    focusOnVarIndex: function(index) {
      this.refs['envVarName' + index].focus();
    },

    onCancel: function() {
      this.setState(this.getInitialState());
      this.props.onCancelClick();
    },

    onChangeVarName: function(index, newName) {
      var vars = this.getVars();
      var newVar = {
        isAlreadySavedWithName: vars[index].isAlreadySavedWithName,
        isAlreadySavedWithValue: false,
        name: formatEnvVarName(newName),
        value: vars[index].value
      };
      this.setState({
        vars: ImmutableObjectUtils.arrayWithNewElementAtIndex(vars, newVar, index)
      });
    },

    onChangeVarValue: function(index, newValue) {
      var vars = this.getVars();
      var newVar = {
        isAlreadySavedWithName: vars[index].isAlreadySavedWithName,
        isAlreadySavedWithValue: false,
        name: vars[index].name,
        value: newValue
      };
      this.setState({
        vars: ImmutableObjectUtils.arrayWithNewElementAtIndex(vars, newVar, index)
      });
    },

    onSave: function() {
      this.props.onSave(this.state.vars);
    },

    resetVar: function(index) {
      var vars = this.getVars();
      var newVar = {
        isAlreadySavedWithName: vars[index].isAlreadySavedWithName,
        isAlreadySavedWithValue: false,
        name: vars[index].name,
        value: ''
      };
      this.setState({
        vars: ImmutableObjectUtils.arrayWithNewElementAtIndex(vars, newVar, index)
      }, function() {
        this.refs['envVarValue' + index].focus();
      });
    },

    getNameInputForVar: function(v, index) {
      if (v.isAlreadySavedWithName) {
        return (
          <input type="text"
            className="form-input form-input-left"
            placeholder="ENVIRONMENT_VARIABLE_NAME"
            value={v.name}
            readOnly={true}
          />
        );
      } else {
        return (
          <Input
            ref={"envVarName" + index}
            className="form-input-left"
            placeholder="Enter name"
            value={v.name}
            onChange={this.onChangeVarName.bind(this, index)}
          />
        );
      }
    },

    getValueInputForVar: function(v, index) {
      if (v.isAlreadySavedWithValue) {
        return (
          <div className="position-relative">
            <input
              className="form-input form-input-right"
              type="text"
              disabled={true}
              value="........"
            />
            <div className="position-absolute position-top-right mts mrm">
              <button type="button" className="button-raw button-s"
                onClick={this.resetVar.bind(this, index)}>Reset</button>
            </div>
          </div>
        );
      } else {
        return (
          <Input
            ref={"envVarValue" + index}
            className="form-input-right"
            placeholder="Enter value"
            value={v.value || ""}
            onChange={this.onChangeVarValue.bind(this, index)}
          />
        );
      }
    },

    render: function() {
      return (
        <div className="box-action">
          <div className="container phn">
            <p>
              <span>Set environment variables to hold secure information like access keys for other services </span>
              <span>that may be used by multiple behaviors.</span>
            </p>

            <div className="form-grouped-inputs">
            {this.getVars().map(function(v, index) {
              return (
                <div className="columns" key={"envVar" + index}>
                  <div className="column column-one-quarter prn">
                    {this.getNameInputForVar(v, index)}
                  </div>
                  <div className="column column-one-quarter pln">
                    {this.getValueInputForVar(v, index)}
                  </div>
                </div>
              );
            }, this)}
            </div>

            <div className="columns mtm">
              <div className="column column-one-half">
                <button type="button"
                  className="button-primary mrs"
                  disabled={!this.hasNameAndValue()}
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
