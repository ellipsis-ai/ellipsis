define(function(require) {
  var React = require('react'),
    ImmutableObjectUtils = require('../lib/immutable_object_utils'),
    Input = require('../form/input'),
    Textarea = require('../form/textarea'),
    formatEnvVarName = require('../lib/formatter').formatEnvironmentVariableName,
    ifPresent = require('../lib/if_present'),
    SetterActions = require('./setter_actions');

  return React.createClass({
    propTypes: {
      onCancelClick: React.PropTypes.func,
      onSave: React.PropTypes.func.isRequired,
      vars: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      errorMessage: React.PropTypes.string,
      isFullPage: React.PropTypes.bool
    },

    getVars: function() {
      return this.state.vars;
    },

    createNewVar: function() {
      return {
        name: "",
        value: "",
        isAlreadySavedWithValue: false
      };
    },

    getInitialState: function() {
      return {
        vars: this.props.vars,
        newVars: [this.createNewVar()],
        saveError: false,
        isSaving: false
      };
    },

    reset: function() {
      this.setState(this.getInitialState());
    },

    isValid: function() {
      return this.hasChanges() && this.getDuplicateNames().length === 0;
    },

    hasChanges: function() {
      return this.hasChangesComparedTo(this.props.vars) || this.getNewVars().some((ea) => !!ea.name);
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

    getNewVars: function() {
      return this.state.newVars;
    },

    setNewVarIndexName: function(index, newName) {
      var previousNewVars = this.getNewVars();
      var newVar = Object.assign({}, previousNewVars[index], { name: formatEnvVarName(newName) });
      var newNewVars = ImmutableObjectUtils.arrayWithNewElementAtIndex(previousNewVars, newVar, index);
      this.setState({ newVars: newNewVars });
    },

    setNewVarIndexValue: function(index, newValue) {
      var previousNewVars = this.state.newVars;
      var newVar = Object.assign({}, previousNewVars[index], { value: newValue });
      var newNewVars = ImmutableObjectUtils.arrayWithNewElementAtIndex(previousNewVars, newVar, index);
      this.setState({ newVars: newNewVars });
    },

    getDuplicateNames: function() {
      return this.getNewVars().filter((newVar) => {
        return newVar.name && this.getVars().some((ea) => ea.name === newVar.name);
      }).map((dupe) => dupe.name);
    },

    addNewVar: function() {
      this.setState({
        newVars: this.state.newVars.concat(this.createNewVar())
      });
    },

    focusOnVarName: function(name) {
      var matchingVarIndex = this.getVars().findIndex((ea) => ea.name === name);
      if (matchingVarIndex >= 0) {
        this.refs[`envVarValue${matchingVarIndex}`].focus();
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
      var newVar = Object.assign({}, vars[index], { value: newValue });
      this.setState({
        vars: ImmutableObjectUtils.arrayWithNewElementAtIndex(vars, newVar, index)
      });
    },

    onSave: function() {
      var namedNewVars = this.getNewVars().filter((ea) => !!ea.name);
      this.setState({
        vars: this.getVars().concat(namedNewVars),
        newVars: [this.createNewVar()],
        saveError: false,
        isSaving: true
      }, () => {
        this.props.onSave(this.state.vars);
      });
    },

    resetVar: function(index) {
      var vars = this.getVars();
      var newVar = Object.assign({}, vars[index], {
        isAlreadySavedWithValue: false,
        value: ''
      });
      this.setState({
        vars: ImmutableObjectUtils.arrayWithNewElementAtIndex(vars, newVar, index)
      }, () => {
        this.refs['envVarValue' + index].focus();
      });
    },

    getValueInputForVar: function(v, index) {
      if (v.isAlreadySavedWithValue) {
        return (
          <div className="position-relative">
            <span className="type-monospace type-weak mrm">
              ••••••••
            </span>
            <span className="type-s">
              <button type="button" className="button-raw"
                onClick={this.resetVar.bind(this, index)}>Clear team-wide value</button>
            </span>
          </div>
        );
      } else {
        return (
          <Textarea
            ref={"envVarValue" + index}
            className="type-monospace form-input-borderless form-input-height-auto"
            placeholder="Set team-wide value (optional)"
            value={v.value || ""}
            onChange={this.onChangeVarValue.bind(this, index)}
            rows={this.getRowCountForTextareaValue(v.value)}
          />
        );
      }
    },

    getRowCountForTextareaValue: function(value) {
      return String(Math.min((value || "").split('\n').length, 5));
    },

    onSaveError: function() {
      this.setState({
        saveError: true,
        isSaving: false
      });
    },

    getDuplicateErrorMessage: function() {
      var names = this.getDuplicateNames();
      if (names.length === 0) {
        return null;
      }
      var errorMessage = (names.length === 1) ?
        `There is already a variable named ${names[0]}.` :
        `These variable names already exist: ${names.join(', ')}`;
      return (
        <span className="mbs type-pink type-s align-button fade-in">{errorMessage}</span>
      );
    },

    render: function() {
      return (
        <div>
            <p>
              <span>Set environment variables to hold secure information like access keys for other services </span>
              <span>that may be used by multiple skills.</span>
            </p>

            <div className="columns">
              <div>
                {this.getVars().map((v, index) => {
                  return (
                    <div className="column-row" key={`envVar${index}`}>
                      <div className="column column-page-sidebar type-monospace pvxs mobile-pbn">
                        <div className={
                          "type-monospace display-ellipsis " +
                          (v.isAlreadySavedWithValue ? "" : "align-button")
                        }>
                          {v.name}
                        </div>
                      </div>
                      <div className="column column-page-main pvxs mobile-ptn">
                        {this.getValueInputForVar(v, index)}
                      </div>
                    </div>
                 );
                }, this)}
              </div>
            </div>

            <hr />

            <div className="columns">
              <div>
                {this.getNewVars().map((v, index) => {
                  return (
                    <div className="column-row" key={`newEnvVar${index}`}>
                      <div className="column column-one-quarter mobile-column-one-half pvxs mobile-phn">
                        <Input
                          className="form-input-borderless type-monospace"
                          placeholder="New variable name"
                          value={v.name}
                          onChange={this.setNewVarIndexName.bind(this, index)}
                        />
                      </div>
                      <div className="column column-three-quarters mobile-column-full pvxs mobile-ptn">
                        <Textarea
                          ref={"newEnvVarValue" + index}
                          className="type-monospace form-input-borderless form-input-height-auto"
                          placeholder="Set team-wide value (optional)"
                          value={v.value || ""}
                          onChange={this.setNewVarIndexValue.bind(this, index)}
                          rows={this.getRowCountForTextareaValue(v.value)}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="align-r mts">
              <button type="button"
                className="button-s"
                onClick={this.addNewVar}
              >
                Add another
              </button>
            </div>

            <SetterActions isFullPage={this.props.isFullPage}>
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
              ), () => (this.getDuplicateErrorMessage()))}
            </SetterActions>
        </div>
      );
    }
  });
});
