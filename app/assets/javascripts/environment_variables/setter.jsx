define(function(require) {
  var React = require('react'),
    Input = require('../form/input'),
    ImmutableObjectUtils = require('../immutable_object_utils'),
    Textarea = require('../form/textarea'),
    formatEnvVarName = require('./formatter');

  return React.createClass({
    propTypes: {
      onCancelClick: React.PropTypes.func,
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

    cancelShouldBeDisabled: function() {
      return !this.props.onCancelClick && !this.hasChanges();
    },

    hasChanges: function() {
      var initialVars = this.getInitialState().vars;
      return !this.getVars().every((ea, index) => {
        return ea.name === initialVars[index].name
          && ea.isAlreadySavedWithName === initialVars[index].isAlreadySavedWithName
          && ea.isAlreadySavedWithValue === initialVars[index].isAlreadySavedWithValue
          && ea.value === initialVars[index].value;
      });
    },

    onCancel: function() {
      this.setState(this.getInitialState());
      if (this.props.onCancelClick) {
        this.props.onCancelClick();
      }
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
          <div className="type-monospace align-button display-ellipsis">
            {v.name}
          </div>
        );
      } else {
        return (
          <Input
            ref={"envVarName" + index}
            className="form-input-borderless"
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
            <span className="align-button type-monospace type-weak mrm">
              ••••••••
            </span>
            <button type="button" className="button-raw mbs"
              onClick={this.resetVar.bind(this, index)}>Reset</button>
          </div>
        );
      } else {
        return (
          <Textarea
            ref={"envVarValue" + index}
            className="type-monospace"
            placeholder="Enter value"
            value={v.value || ""}
            onChange={this.onChangeVarValue.bind(this, index)}
          />
        );
      }
    },

    render: function() {
      return (
        <div>
            <p>
              <span>Set environment variables to hold secure information like access keys for other services </span>
              <span>that may be used by multiple behaviors.</span>
            </p>

            <div className="columns columns-elastic mobile-columns-float">
              <div className="column-group">
                {this.getVars().map(function(v, index) {
                  return (
                    <div className="column-row" key={"envVar" + index}>
                      <div className="column column-shrink mobile-column-full type-monospace pvs mobile-pbn">
                        {this.getNameInputForVar(v, index)}
                      </div>
                      <div className="column column-expand pvs mobile-ptn">
                        {this.getValueInputForVar(v, index)}
                      </div>
                    </div>
                 );
                }, this)}
              </div>
            </div>

            <div className="mtxl">
              <button type="button"
                className="button-primary mrs mbs"
                disabled={!this.hasNameAndValue()}
                onClick={this.onSave}
              >Save</button>
              <button className="mbs"
                type="button"
                onClick={this.onCancel}
                disabled={this.cancelShouldBeDisabled()}
              >
                Cancel
              </button>
            </div>

        </div>
      );
    }
  });
});
