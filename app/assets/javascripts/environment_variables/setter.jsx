define(function(require) {
  var React = require('react'),
    ImmutableObjectUtils = require('../immutable_object_utils'),
    Input = require('../form/input'),
    Textarea = require('../form/textarea'),
    formatEnvVarName = require('./formatter'),
    ifPresent = require('../if_present');

  return React.createClass({
    propTypes: {
      onCancelClick: React.PropTypes.func,
      onSave: React.PropTypes.func.isRequired,
      vars: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      errorMessage: React.PropTypes.string
    },

    getVars: function() {
      return this.state.vars;
    },

    getInitialState: function() {
      return {
        vars: this.props.vars.sort((a, b) => {
          var aName = a.name.toLowerCase();
          var bName = b.name.toLowerCase();
          if (aName < bName) {
            return -1;
          } else if (aName > bName) {
            return 1;
          } else {
            return 0;
          }
        }),
        newVarName: "",
        saveError: false,
        isSaving: false
      };
    },

    reset: function() {
      this.setState(this.getInitialState());
    },

    hasChanges: function() {
      return this.hasChangesComparedTo(this.props.vars) || !!this.getNewVarName();
    },

    hasChangesComparedTo: function(oldVars) {
      return !this.getVars().every((v, index) => {
        var theVar = oldVars[index];
        return theVar &&
          v.name === theVar.name &&
          v.value === theVar.value &&
          v.isAlreadySavedWithValue === theVar.isAlreadySavedWithValue;
      });
    },

    getNewVarName: function() {
      return this.state.newVarName;
    },

    setNewVarName: function(newName) {
      this.setState({ newVarName: formatEnvVarName(newName) });
    },

    newNameIsDuplicate: function() {
      var newName = this.getNewVarName();
      return !!newName && !this.getVars().every((ea) => ea.name !== newName);
    },

    hasAcceptableNewVarName: function() {
      return !!this.getNewVarName() && !this.newNameIsDuplicate();
    },

    addNewVar: function() {
      this.setState({
        vars: this.getVars().concat({
          isAlreadySavedWithValue: false,
          name: this.getNewVarName(),
          value: ""
        }),
        newVarName: ""
      }, () => {
        this.focusOnVarIndex(this.getVars().length - 1);
      });
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
      this.refs['envVarValue' + index].focus();
    },

    cancelShouldBeDisabled: function() {
      return !this.props.onCancelClick && !this.hasChanges();
    },

    onCancel: function() {
      this.setState(this.getInitialState());
      if (this.props.onCancelClick) {
        this.props.onCancelClick();
      }
    },

    onChangeVarValue: function(index, newValue) {
      var vars = this.getVars();
      var newVar = {
        isAlreadySavedWithValue: false,
        name: vars[index].name,
        value: newValue
      };
      this.setState({
        vars: ImmutableObjectUtils.arrayWithNewElementAtIndex(vars, newVar, index)
      });
    },

    onSave: function() {
      this.setState({
        saveError: false,
        isSaving: true
      }, () => {
        this.props.onSave(this.state.vars);
      });
    },

    resetVar: function(index) {
      var vars = this.getVars();
      var newVar = {
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

    onSaveError: function() {
      this.setState({
        saveError: true,
        isSaving: false
      });
    },

    render: function() {
      return (
        <div>
            <p>
              <span>Set environment variables to hold secure information like access keys for other services </span>
              <span>that may be used by multiple behaviors.</span>
            </p>

            <div className="columns">
              <div className="column-group">
                {this.getVars().map(function(v, index) {
                  return (
                    <div className="column-row" key={"envVar" + index}>
                      <div className="column column-one-quarter mobile-column-full type-monospace pvxs mobile-pbn">
                        <div className="type-monospace align-button display-ellipsis">
                          {v.name}
                        </div>
                      </div>
                      <div className="column column-three-quarters mobile-column-full pvxs mobile-ptn">
                        {this.getValueInputForVar(v, index)}
                      </div>
                    </div>
                 );
                }, this)}
                <div className="column-row">
                  <div className="column column-one-quarter mobile-column-one-half pvxs mobile-phn">
                    <Input
                      className="form-input-borderless type-monospace"
                      placeholder="New variable name"
                      value={this.getNewVarName()}
                      onChange={this.setNewVarName}
                    />
                  </div>
                  <div className="column column-three-quarters mobile-column-full pvxs mobile-ptn">
                    <button type="button"
                      className="button-s mts mrl"
                      disabled={!this.hasAcceptableNewVarName()}
                      onClick={this.addNewVar}
                    >Add new variable</button>
                    {ifPresent(this.newNameIsDuplicate(), () => (
                      <span className="type-pink type-s fade-in">
                        There is already a variable named {this.getNewVarName()}.
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            <div className="mtxl">
              <button type="button"
                className={"button-primary mrs mbs " + (this.state.isSaving ? "button-activated" : "")}
                disabled={!this.hasChanges()}
                onClick={this.onSave}
              >
                <span className="button-labels">
                  <span className="button-normal-label">Save</span>
                  <span className="button-activated-label">Saving…</span>
                </span>
              </button>
              <button className="mbs mrl"
                type="button"
                onClick={this.onCancel}
                disabled={this.cancelShouldBeDisabled()}
              >
                Cancel
              </button>
              {ifPresent(this.state.saveError, () => (
                <span className="mbs type-pink type-bold align-button fade-in">
                  An error occurred while saving. Please try again.
                </span>
              ))}
            </div>

        </div>
      );
    }
  });
});
