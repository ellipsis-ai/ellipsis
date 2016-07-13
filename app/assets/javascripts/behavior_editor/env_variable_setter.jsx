define(function(require) {
  var React = require('react'),
    Input = require('./input'),
    ImmutableObjectUtils = require('../immutable_object_utils');

  return React.createClass({
    propTypes: {
      onCancelClick: React.PropTypes.func.isRequired,
      vars: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    getVars: function() {
      return this.state.vars;
    },

    getInitialState: function() {
      return {
        vars: this.props.vars
      }
    },

    hasChanges: function() {
      return !this.getVars().every(function(v, index) {
        return v.value === this.props.vars[index].value && v.isSet === this.props.vars[index].isSet;
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

    onCancel: function() {
      this.setState(this.getInitialState());
      this.props.onCancelClick();
    },

    onChangeVarValue: function(modifiedIndex, newValue) {
      var vars = this.getVars();
      var newVar = {
        isSet: false,
        name: vars[modifiedIndex].name,
        value: newValue
      };
      this.setState({
        vars: ImmutableObjectUtils.arrayWithNewElementAtIndex(vars, newVar, modifiedIndex)
      });
    },

    resetVar: function(index) {
      var vars = this.getVars();
      var newVar = {
        isSet: false,
        name: vars[index].name,
        value: ''
      };
      this.setState({
        vars: ImmutableObjectUtils.arrayWithNewElementAtIndex(vars, newVar, index)
      }, function() {
        this.refs['envVarValue' + index].focus();
      });
    },

    getInputForVar: function(v, index) {
      if (v.isSet) {
        return (
          <div className="position-relative">
            <input
              className="form-input form-input-right"
              type="password"
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
            value={v.value}
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
                    <input type="text"
                      className="form-input form-input-left"
                      placeholder="ENVIRONMENT_VARIABLE_NAME"
                      value={v.name}
                      readOnly={true}
                    />
                  </div>
                  <div className="column column-one-quarter pln">
                    {this.getInputForVar(v, index)}
                  </div>
                </div>
              );
            }, this)}
            </div>

            <div className="columns mtm">
              <div className="column column-one-half">
                <button type="button"
                  className="button-primary mrs"
                  disabled={!this.hasChanges()}
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
