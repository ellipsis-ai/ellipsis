define(function(require) {
var React = require('react'),
  ReactDOM = require('react-dom'),
  Codemirror = require('./react-codemirror'),
  CodemirrorJSMode = require('./codemirror/mode/javascript/javascript'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorBoilerplateParameterHelp = require('./behavior_editor_boilerplate_parameter_help'),
  BehaviorEditorCodeHeader = require('./behavior_editor_code_header'),
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
    responseTemplate: React.PropTypes.string,
    params: React.PropTypes.arrayOf(React.PropTypes.shape({
      name: React.PropTypes.string.isRequired,
      question: React.PropTypes.string.isRequired
    })),
    triggers: React.PropTypes.arrayOf(React.PropTypes.string),
    csrfToken: React.PropTypes.string.isRequired,
    envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string)
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

  isNewBehavior: function() {
    return !this.props.nodeFunction;
  },

  getInitialState: function() {
    return {
      behavior: {
        behaviorId: this.props.behaviorId,
        description: this.props.description,
        nodeFunction: this.props.nodeFunction,
        responseTemplate: this.props.responseTemplate,
        params: this.props.params,
        triggers: this.props.triggers.concat(['']) // always add one blank trigger
      },
      codeEditorUseLineWrapping: false,
      settingsMenuVisible: false,
      boilerplateHelpVisible: false,
      expandEnvVariables: false,
      envVariableNames: this.props.envVariableNames
    };
  },

  getBehaviorProp: function(key) {
    return this.state.behavior[key];
  },

  getBehaviorDescription: function() {
    return this.getBehaviorProp('description');
  },

  getBehaviorNodeFunction: function() {
    return this.getBehaviorProp('nodeFunction');
  },

  getBehaviorParams: function() {
    return this.getBehaviorProp('params');
  },

  getBehaviorTemplate: function() {
    return this.getBehaviorProp('responseTemplate') || this.getDefaultBehaviorTemplate();
  },

  getBehaviorTriggers: function() {
    return this.getBehaviorProp('triggers');
  },

  getDefaultBehaviorTemplate: function() {
    var result = '',
      params = this.getBehaviorParams();

    params.forEach(function(param) {
      var name = param.name ?
        '{' + param.name + '}' :
        '(blank parameter name)';

      var question = param.question ?
        '“' + param.question + '”' :
        '(blank question)';

      result += 'You said ' + name + ' for ' + question + '.\n';
    });

    if (params.length) {
      result += '\n';
    }

    result += 'The answer is: {successResponse}.\n';

    return result;
  },

  setBehaviorProp: function(key, value, callback) {
    var newData = this.utils.objectWithNewValueAtKey(this.state.behavior, value, key);
    this.setState({ behavior: newData }, callback);
  },

  hasParams: function() {
    return this.getBehaviorParams().length > 0;
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
    this.setBehaviorProp('triggers', this.getBehaviorTriggers().concat(['']), this.focusOnFirstBlankTrigger);
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
    var triggers = this.utils.arrayRemoveElementAtIndex(this.getBehaviorTriggers(), index);
    if (index == triggers.length) {
      // Add a blank trigger on the end if the user deleted the last trigger
      triggers = triggers.concat(['']);
    }
    this.setBehaviorProp('triggers', triggers);
  },

  focusOnParamIndex: function(index) {
    this.refs.codeHeader.focusIndex(index);
  },

  focusOnLastParam: function() {
    this.focusOnParamIndex(this.getBehaviorParams().length - 1)
  },

  onDescriptionChange: function(newDescription) {
    this.setBehaviorProp('description', newDescription);
  },

  onCodeChange: function(newCode) {
    this.setBehaviorProp('nodeFunction', newCode);
  },

  onTemplateChange: function(newTemplateString) {
    this.setBehaviorProp('responseTemplate', newTemplateString)
  },

  addParam: function() {
    var newParamIndex = this.getBehaviorParams().length + 1;
    while (this.getBehaviorParams().some(function(param) {
      return param.name == 'userInput' + newParamIndex;
    })) {
      newParamIndex++;
    }
    var newParams = this.getBehaviorParams().concat([{ name: 'userInput' + newParamIndex, question: '' }]);
    this.setBehaviorProp('params', newParams, this.focusOnLastParam);
  },

  replaceParamAtIndexWithParam: function(index, newParam) {
    this.setBehaviorProp('params', this.utils.arrayWithNewElementAtIndex(this.getBehaviorParams(), newParam, index));
  },

  deleteParamAtIndex: function(index) {
    this.setBehaviorProp('params', this.utils.arrayRemoveElementAtIndex(this.getBehaviorParams(), index));
  },

  onParamEnterKey: function(index) {
    if (index + 1 < this.getBehaviorParams().length) {
      this.focusOnParamIndex(index + 1);
    } else if (this.getBehaviorParams()[index].question != '') {
      this.addParam();
    }
  },

  onTriggerEnterKey: function(index) {
    if (index + 1 < this.getBehaviorTriggers().length) {
      this.refs['trigger' + (index + 1)].focus();
    } else if (this.getBehaviorTriggers()[index] != '') {
      this.addTrigger();
    }
  },

  onTriggerChange: function(index, newTrigger) {
    this.setBehaviorProp('triggers', this.utils.arrayWithNewElementAtIndex(this.getBehaviorTriggers(), newTrigger, index));
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

        <div className="bg-light">
          <div className="container ptxl pbm">
            <h3 className="man type-weak">
              <span>Edit behavior</span>
              <span className={"type-italic type-pink" + this.visibleWhen(this.isModified())}>— unsaved changes</span>
            </h3>
          </div>
        </div>

        {/* Start of container */}
        <div className="container ptxl pbm">

          <div className="form-field-group">
            <BehaviorEditorInput
              className="form-input-borderless form-input-h2"
              placeholder="Describe the behavior in one phrase"
              value={this.getBehaviorDescription()}
              onChange={this.onDescriptionChange}
            />
          </div>

          <div className="columns">
            <div className="column column-one-quarter form-field-group">

              <p>
                <strong>Implement the behavior by writing a node.js function.</strong>
              </p>

              <ul className="type-s">
                <li className="mbs">
                  <span>Call onSuccess with a response.</span>
                </li>

                <li className={"mbs" + this.visibleWhen(!this.getBehaviorParams().length)}>
                  If you need to collect information from the user, add one or more parameters
                  to your function.
                </li>
              </ul>

            </div>

            <div className="column column-three-quarters pll form-field-group">

              <BehaviorEditorCodeHeader
                ref="codeHeader"
                hasParams={this.hasParams()}
                params={this.getBehaviorParams()}
                onParamChange={this.replaceParamAtIndexWithParam}
                onParamDelete={this.deleteParamAtIndex}
                onParamAdd={this.addParam}
                onEnterKey={this.onParamEnterKey}
                helpVisible={this.state.boilerplateHelpVisible}
                onToggleHelp={this.toggleBoilerplateHelp}
              />

              <div
                className={this.visibleWhen(this.state.boilerplateHelpVisible, true)}
                ref="boilerplateHelpContainer"
              >
                <BehaviorEditorBoilerplateParameterHelp
                  envVariableNames={this.state.envVariableNames}
                  onExpandToggle={this.toggleEnvVariableExpansion}
                  expandEnvVariables={this.state.expandEnvVariables}
                  onCollapseClick={this.toggleBoilerplateHelp}
                />
              </div>

              <div className="position-relative pr-symbol border-right">
                <Codemirror value={this.getBehaviorNodeFunction()}
                  onChange={this.onCodeChange}
                  options={{
                    mode: "javascript",
                    firstLineNumber: 2,
                    indentUnit: 2,
                    indentWithTabs: false,
                    lineWrapping: this.state.codeEditorUseLineWrapping,
                    lineNumbers: true,
                    smartIndent: true,
                    tabSize: 2,
                    viewportMargin: Infinity,
                    extraKeys: {
                      Tab: function(cm) {
                        var spaces = Array(cm.getOption("indentUnit") + 1).join(" ");
                        cm.replaceSelection(spaces);
                      }
                    }
                  }}
                />
                <div className="position-absolute position-top-right">
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

              <div className="border-left border-bottom border-right border-radius-bottom pvs plxl">
                <code className="type-weak type-s">{"}"}</code>
              </div>

            </div>
          </div>

          <div className="columns">

            <div className="column column-one-quarter mbxl">

              <p>
                <strong>Format the response</strong>
              </p>

              <ul className="type-s">
                <li className="mbs">
                  <span>Use Markdown for structure and presentation</span>
                </li>

                <li className="mbs">
                  You can include all user-supplied parameters, plus a special successResponse variable.
                </li>
              </ul>

            </div>

            <div className="column column-three-quarters pll mbxl">
              <div className="border-top border-left border-right border-radius-top ptxl"></div>
              <div className="position-relative pr-symbol border-right">
                <Codemirror value={this.getBehaviorTemplate()}
                  onChange={this.onTemplateChange}
                  options={{
                    mode: "markdown",
                    gutters: ['CodeMirror-empty-gutter'],
                    indentUnit: 4,
                    indentWithTabs: true,
                    lineWrapping: true,
                    lineNumbers: false,
                    smartIndent: true,
                    tabSize: 4,
                    viewportMargin: Infinity,
                    placeholder: "The result is {successResponse}"
                  }}
                />
              </div>
              <div className="border-bottom border-left border-right border-radius-bottom ptxl"></div>
            </div>
          </div>

          <hr className="mtn" />

          <div className="columns form-field-group">
            <div className="column column-one-quarter">

              <p><strong>Specify one or more phrases to trigger this behavior in chat.</strong></p>

              {/*<p>
                <span>You can write triggers using regular expressions to collect user input from the trigger, </span>
                <span> or to be more flexible.</span>
              </p>*/}

            </div>

            <div className="column column-three-quarters pll form-field-group">

              <div className="form-grouped-inputs mbl">
              {this.getBehaviorTriggers().map(function(trigger, index) {
                return (
                  <BehaviorEditorTriggerInput
                    key={"BehaviorEditorTrigger" + index}
                    ref={"trigger" + index}
                    value={trigger}
                    onChange={this.onTriggerChange.bind(this, index)}
                    onDelete={this.deleteTriggerAtIndex.bind(this, index)}
                    onEnterKey={this.onTriggerEnterKey.bind(this, index)}
                    mayHideDelete={index + 1 == this.getBehaviorTriggers().length}
                  />
                );
              }, this)}
              </div>
              <div className="pr-symbol align-r">
                <button type="button" className="button-s" onClick={this.addTrigger}>Add another trigger</button>
              </div>

            </div>
          </div>

        </div> {/* End of container */}

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
  load: function(data, containerId, csrfToken, envVariableNames) {
    var additionalData = { csrfToken: csrfToken, envVariableNames: envVariableNames };
    var myBehaviorEditor = React.createElement(BehaviorEditor, Object.assign(data, additionalData));
    ReactDOM.render(myBehaviorEditor, document.getElementById(containerId));
  }
};

});
