define(function(require) {
var React = require('react'),
  ReactDOM = require('react-dom'),
  Codemirror = require('./react-codemirror'),
  CodemirrorJSMode = require('./codemirror/mode/javascript/javascript'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorBoilerplateParameterHelp = require('./behavior_editor_boilerplate_parameter_help'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorHelpButton = require('./behavior_editor_help_button'),
  BehaviorEditorHiddenJsonInput = require('./behavior_editor_hidden_json_input'),
  BehaviorEditorInput = require('./behavior_editor_input'),
  BehaviorEditorSettingsButton = require('./behavior_editor_settings_button'),
  BehaviorEditorSettingsMenu = require('./behavior_editor_settings_menu'),
  BehaviorEditorTriggerInput = require('./behavior_editor_trigger_input'),
  BehaviorEditorUserInputDefinition = require('./behavior_editor_user_input_definition'),
  CsrfTokenHiddenInput = require('./csrf_token_hidden_input');

var BehaviorEditor = React.createClass({
  displayName: 'BehaviorEditor',
  mixins: [BehaviorEditorMixin],

  propTypes: {
    behaviorId: React.PropTypes.string,
    description: React.PropTypes.string,
    nodeFunction: React.PropTypes.string,
    params: React.PropTypes.arrayOf(React.PropTypes.shape({
      name: React.PropTypes.string.isRequired,
      question: React.PropTypes.string.isRequired
    })),
    triggers: React.PropTypes.arrayOf(React.PropTypes.string),
    csrfToken: React.PropTypes.string.isRequired
  },

  utils: {
    // Create a copy of an array before modifying it
    arrayWithNewElementAtIndex: function(array, newElement, index) {
      var newArray = array.slice();
      newArray[index] = newElement;
      return newArray;
    },

    arrayRemoveElementAtIndex: function(array, index) {
      var newArray = array.slice();
      newArray.splice(index, 1);
      return newArray;
    },

    objectWithNewValueAtKey: function(obj, newValue, keyToChange) {
      var newObj = {};
      Object.keys(obj).forEach(function(key) {
        if (key === keyToChange) {
          newObj[key] = newValue;
        } else {
          newObj[key] = obj[key];
        }
      });
      return newObj;
    }
  },

  getInitialState: function() {
    return {
      behavior: {
        behaviorId: this.props.behaviorId,
        description: this.props.description,
        nodeFunction: this.props.nodeFunction,
        params: this.props.params,
        triggers: this.props.triggers.concat(['']) // always add one blank trigger
      },
      codeEditorUseLineWrapping: false,
      settingsMenuVisible: false,
      boilerplateHelpVisible: false,
      expandEnvVariables: false,
      envVariableNames: ['AWS_ACCESS_KEY', 'AWS_SECRET_KEY', 'ANOTHER_KEY', 'AND_ANOTHER_KEY'] // TODO: make these real
    };
  },

  getBehaviorProp: function(key) {
    return this.state.behavior[key];
  },

  setBehaviorProp: function(key, value, callback) {
    var newData = this.utils.objectWithNewValueAtKey(this.state.behavior, value, key);
    this.setState({ behavior: newData }, callback);
  },

  isModified: function() {
    return JSON.stringify(this.state.behavior) !== JSON.stringify(this.getInitialState().behavior);
  },

  undoChanges: function() {
    if (window.confirm("Are you sure you want to undo changes?")) {
      this.setState({ behavior: this.getInitialState().behavior });
    }
  },

  addTrigger: function() {
    this.setBehaviorProp('triggers', this.getBehaviorProp('triggers').concat(['']), this.focusOnFirstBlankTrigger);
  },

  focusOnFirstBlankTrigger: function() {
    var blankTrigger = Object.keys(this.refs).find(function(key) {
      return key.match(/^trigger\d+$/) && this.refs[key].isEmpty();
    }, this);
    if (blankTrigger) {
      this.refs[blankTrigger].focus();
    }
  },

  deleteTriggerAtIndex: function(index) {
    var triggers = this.utils.arrayRemoveElementAtIndex(this.getBehaviorProp('triggers'), index);
    if (index == triggers.length) {
      // Add a blank trigger on the end if the user deleted the last trigger
      triggers = triggers.concat(['']);
    }
    this.setBehaviorProp('triggers', triggers);
  },

  focusOnLastParam: function() {
    this.refs['param' + (this.getBehaviorProp('params').length - 1)].focus();
  },

  onDescriptionChange: function(newDescription) {
    this.setBehaviorProp('description', newDescription);
  },

  onCodeChange: function(newCode) {
    this.setBehaviorProp('nodeFunction', newCode);
  },

  addParam: function() {
    var newParamIndex = this.getBehaviorProp('params').length + 1;
    while (this.getBehaviorProp('params').some(function(param) {
      return param.name == 'userInput' + newParamIndex;
    })) {
      newParamIndex++;
    }
    var newParams = this.getBehaviorProp('params').concat([{ name: 'userInput' + newParamIndex, question: '' }]);
    this.setBehaviorProp('params', newParams, this.focusOnLastParam);
  },

  replaceParamAtIndexWithParam: function(index, newParam) {
    this.setBehaviorProp('params', this.utils.arrayWithNewElementAtIndex(this.getBehaviorProp('params'), newParam, index));
  },

  deleteParamAtIndex: function(index) {
    this.setBehaviorProp('params', this.utils.arrayRemoveElementAtIndex(this.getBehaviorProp('params'), index));
  },

  onParamEnterKey: function(index) {
    if (index + 1 < this.getBehaviorProp('params').length) {
      this.refs['param' + (index + 1)].focus();
    } else if (this.getBehaviorProp('params')[index].question != '') {
      this.addParam();
    }
  },

  onTriggerEnterKey: function(index) {
    if (index + 1 < this.getBehaviorProp('triggers').length) {
      this.refs['trigger' + (index + 1)].focus();
    } else if (this.getBehaviorProp('triggers')[index] != '') {
      this.addTrigger();
    }
  },

  onTriggerChange: function(index, newTrigger) {
    this.setBehaviorProp('triggers', this.utils.arrayWithNewElementAtIndex(this.getBehaviorProp('triggers'), newTrigger, index));
  },

  toggleEditorSettingsMenu: function() {
    this.setState({
      settingsMenuVisible: !this.state.settingsMenuVisible
    });
  },

  toggleCodeEditorLineWrapping: function() {
    this.setState({
      codeEditorUseLineWrapping: !this.state.codeEditorUseLineWrapping
    });
  },

  toggleBoilerplateHelp: function() {
    /* Reset height of the container to its only child
       before toggling it so the animation is smooth */

    var container = this.refs.boilerplateHelpContainer;
    container.style.maxHeight = container.children[0].offsetHeight + 'px';

    this.setState({
      boilerplateHelpVisible: !this.state.boilerplateHelpVisible
    });
  },

  toggleEnvVariableExpansion: function() {
    /* Reset max height of the container so it can expand */

    var container = this.refs.boilerplateHelpContainer;
    container.style.maxHeight = 'none';

    this.setState({
      expandEnvVariables: !this.state.expandEnvVariables
    });
  },

  onSaveClick: function() {
    this.setState({
      isSaving: true
    });
  },

  render: function() {
    return (
      <form action="/save_behavior" method="POST">
        <CsrfTokenHiddenInput
          value={this.props.csrfToken}
        />
        <BehaviorEditorHiddenJsonInput
          value={JSON.stringify(this.state.behavior)}
        />
        <div className="form-field-group">
          <h3 className="mtxxxl mbn type-weak">
            <span>
              Edit behavior
            </span> <span className={"type-italic type-pink" + this.visibleWhen(this.isModified())}>— unsaved changes</span>
          </h3>
          <BehaviorEditorInput
            className="form-input-borderless form-input-h2"
            placeholder="Describe the behavior in one phrase"
            value={this.getBehaviorProp('description')}
            onChange={this.onDescriptionChange}
          />
        </div>

        <div className="form-field-group">
          <p><strong>Implement the behavior by writing a node.js function.</strong></p>

          <p>If you need to collect information from the user, add one or more parameters
          to your function. For each one, include a question for @ellipsis to ask the user.</p>

          <div>
            <div>
              <code className="type-weak type-s">{"function ("}</code>
            </div>
            <div className="plxl">
              {this.getBehaviorProp('params').map(function(param, index) {
                return (
                  <BehaviorEditorUserInputDefinition
                    key={'BehaviorEditorUserInputDefinition' + index}
                    ref={'param' + index}
                    name={param.name}
                    question={param.question}
                    onChange={this.replaceParamAtIndexWithParam.bind(this, index)}
                    onDelete={this.deleteParamAtIndex.bind(this, index)}
                    onEnterKey={this.onParamEnterKey.bind(this, index)}
                    hasMargin={index > 0}
                    id={index}
                  />
                );
              }, this)}
            </div>
            <div className="columns plxl">
              <div className="column column-one-quarter">
                <div className="columns columns-elastic">
                  <div className="column column-expand">
                    <code className="type-weak type-s">onSuccess,</code><br />
                    <code className="type-weak type-s">onError,</code><br />
                    <code className="type-weak type-s">ellipsis</code>
                  </div>
                  <div className="column column-shrink align-m prl">
                    <BehaviorEditorHelpButton onClick={this.toggleBoilerplateHelp} inverted={this.state.boilerplateHelpVisible} />
                  </div>
                </div>
              </div>
              <div className="column column-three-quarters ptl align-c">
                <button type="button" onClick={this.addParam}>Add parameter</button>
              </div>
              <div
                className={"column column-full" + this.visibleWhen(this.state.boilerplateHelpVisible, true)}
                ref="boilerplateHelpContainer"
              >
                <BehaviorEditorBoilerplateParameterHelp
                  envVariableNames={this.state.envVariableNames}
                  onExpandToggle={this.toggleEnvVariableExpansion}
                  expandEnvVariables={this.state.expandEnvVariables}
                />
              </div>
            </div>
            <div>
              <code className="type-weak type-s">{") {"}</code>
            </div>
            <div className="position-relative prxxxl plxl">
              <Codemirror value={this.getBehaviorProp('nodeFunction')}
                onChange={this.onCodeChange}
                options={{
                  mode: "javascript",
                  lineWrapping: this.state.codeEditorUseLineWrapping,
                  viewportMargin: Infinity
                }}
              />
              <div className="position-absolute position-top-right phxs">
                <BehaviorEditorSettingsButton
                  onClick={this.toggleEditorSettingsMenu}
                  buttonActive={this.state.settingsMenuVisible}
                />
                <BehaviorEditorSettingsMenu isVisible={this.state.settingsMenuVisible} onItemClick={this.toggleEditorSettingsMenu}>
                  <button type="button" className="button-invisible" onMouseUp={this.toggleCodeEditorLineWrapping}>
                    <span className={this.visibleWhen(this.state.codeEditorUseLineWrapping)}>✓</span>
                    <span> Enable line wrap</span>
                  </button>
                </BehaviorEditorSettingsMenu>
              </div>
            </div>
            <div>
              <code className="type-weak type-s">{"}"}</code>
            </div>
          </div>
        </div>

        <div className="form-field-group">
          <p><strong>Specify one or more phrases to trigger this behavior in chat.</strong></p>
          <p>You can use regular expressions for more flexibility and to capture user input.</p>
          <div className="form-grouped-inputs mbl">
          {this.getBehaviorProp('triggers').map(function(trigger, index) {
            return (
              <BehaviorEditorTriggerInput
                key={"BehaviorEditorTrigger" + index}
                ref={"trigger" + index}
                value={trigger}
                onChange={this.onTriggerChange.bind(this, index)}
                onDelete={this.deleteTriggerAtIndex.bind(this, index)}
                onEnterKey={this.onTriggerEnterKey.bind(this, index)}
                mayHideDelete={index + 1 == this.getBehaviorProp('triggers').length}
              />
            );
          }, this)}
          </div>
          <button type="button" onClick={this.addTrigger}>Add another trigger</button>
        </div>

        <div className="ptxl"></div>

        <footer className={"position-fixed-bottom pvm border-top " +
          (this.isModified() ? "bg-white" : "bg-light-translucent")}
        >
          <div className="container">
            <button type="submit"
              className={"button-primary mrs " + (this.state.isSaving ? "button-activated" : "")}
              disabled={!this.isModified()}
              onClick={this.onSaveClick}
            >
              <span className="button-labels">
                <span className="button-normal-label">Save changes</span>
                <span className="button-activated-label">Saving…</span>
              </span>
            </button>
            <button type="button" disabled={!this.isModified()} onClick={this.undoChanges}>Undo changes</button>
          </div>
        </footer>

      </form>
    );
  }
});

return {
  load: function(data, containerId, csrfToken) {
    var myBehaviorEditor = React.createElement(BehaviorEditor, Object.assign(data, {csrfToken: csrfToken}));
    ReactDOM.render(myBehaviorEditor, document.getElementById(containerId));
  }
};

});
