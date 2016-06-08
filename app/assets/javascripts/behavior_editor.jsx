define(function(require) {
var React = require('react'),
  ReactDOM = require('react-dom'),
  Codemirror = require('./react-codemirror'),
  CodemirrorMarkdownMode = require('./codemirror/mode/markdown/markdown'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorBoilerplateParameterHelp = require('./behavior_editor_boilerplate_parameter_help'),
  BehaviorEditorChecklist = require('./behavior_editor_checklist'),
  BehaviorEditorCodeEditor = require('./behavior_editor_code_editor'),
  BehaviorEditorCodeFooter = require('./behavior_editor_code_footer'),
  BehaviorEditorCodeHeader = require('./behavior_editor_code_header'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorHelpButton = require('./behavior_editor_help_button'),
  BehaviorEditorHiddenJsonInput = require('./behavior_editor_hidden_json_input'),
  BehaviorEditorInput = require('./behavior_editor_input'),
  BehaviorEditorSectionHeading = require('./behavior_editor_section_heading'),
  BehaviorEditorSettingsButton = require('./behavior_editor_settings_button'),
  BehaviorEditorSettingsMenu = require('./behavior_editor_settings_menu'),
  BehaviorEditorTriggerHelp = require('./behavior_editor_trigger_help'),
  BehaviorEditorTriggerInput = require('./behavior_editor_trigger_input'),
  BehaviorEditorUserInputDefinition = require('./behavior_editor_user_input_definition'),
  Collapsible = require('./collapsible'),
  CsrfTokenHiddenInput = require('./csrf_token_hidden_input');

var BehaviorEditor = React.createClass({
  displayName: 'BehaviorEditor',
  mixins: [BehaviorEditorMixin],

  propTypes: {
    teamId: React.PropTypes.string.isRequired,
    behaviorId: React.PropTypes.string,
    nodeFunction: React.PropTypes.string,
    responseTemplate: React.PropTypes.string,
    params: React.PropTypes.arrayOf(React.PropTypes.shape({
      name: React.PropTypes.string.isRequired,
      question: React.PropTypes.string.isRequired
    })),
    triggers: React.PropTypes.arrayOf(React.PropTypes.string),
    csrfToken: React.PropTypes.string.isRequired,
    envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string),
    shouldRevealCodeEditor: React.PropTypes.bool
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

  isExistingBehavior: function() {
    return !!(this.props.nodeFunction || this.props.responseTemplate);
  },

  shouldRevealCodeEditor: function() {
    return !!(this.props.shouldRevealCodeEditor || this.props.nodeFunction)
  },

  getMagic8BallResponse: function() {
    var responses = [
      "Reply hazy try again",
      "Ask again later",
      "Better not tell you now",
      "Cannot predict now",
      "Concentrate and ask again"
    ];

    var rand = Math.floor(Math.random() * responses.length);
    return "The magic 8-ball says:\n\n“" + responses[rand] + "”";
  },

  getInitialTriggers: function() {
    if (this.props.triggers && this.props.triggers.length > 0) {
      return this.props.triggers;
    } else {
      return [""];
    }
  },

  getInitialState: function() {
    return {
      behavior: {
        teamId: this.props.teamId,
        behaviorId: this.props.behaviorId,
        nodeFunction: this.props.nodeFunction,
        responseTemplate: this.props.responseTemplate,
        params: this.props.params,
        triggers: this.getInitialTriggers()
      },
      codeEditorUseLineWrapping: false,
      settingsMenuVisible: false,
      boilerplateHelpVisible: false,
      expandEnvVariables: false,
      envVariableNames: this.props.envVariableNames,
      revealCodeEditor: this.shouldRevealCodeEditor(),
      magic8BallResponse: this.getMagic8BallResponse(),
      triggerHelpVisible: false,
      hasModifiedTemplate: !!this.props.responseTemplate
    };
  },

  getBehaviorProp: function(key) {
    return this.state.behavior[key];
  },

  getBehaviorNodeFunction: function() {
    return this.getBehaviorProp('nodeFunction');
  },

  getBehaviorParams: function() {
    return this.getBehaviorProp('params');
  },

  getBehaviorTemplate: function() {
    var template = this.getBehaviorProp('responseTemplate');
    if (!template && !this.state.hasModifiedTemplate) {
      return this.getDefaultBehaviorTemplate();
    } else {
      return template;
    }
  },

  getBehaviorTriggers: function() {
    return this.getBehaviorProp('triggers');
  },

  hasCode: function() {
    return this.getBehaviorNodeFunction().match(/\S/);
  },

  hasCalledOnSuccess: function() {
    var code = this.getBehaviorNodeFunction();
    var success = code && code.match(/\bonSuccess\([\s\S]+?\)/);
    return success;
  },

  hasPrimaryTrigger: function() {
    var triggers = this.getBehaviorTriggers();
    return triggers.length > 0 && triggers[0];
  },

  hasMultipleTriggers: function() {
    return this.getBehaviorTriggers().length > 1;
  },

  triggersUseParams: function() {
    return this.getBehaviorTriggers().some(function(trigger) {
      return trigger.match(/{.+}/);
    });
  },

  getDefaultBehaviorTemplate: function() {
    var result = '',
      params = this.getBehaviorParams();

    params.forEach(function(param) {
      var name = param.name ? '{'+param.name+'}' : '(blank parameter name)';
      var question = param.question ? '“'+param.question+'”' : '(blank question)';
      result += 'You said ' + name + ' for ' + question + '.\n';
    });

    if (params.length) {
      result += '\n';
    }

    if (this.hasCalledOnSuccess()) {
      result += 'The answer is: {successResult}.';
    } else {
      result += this.state.magic8BallResponse;
    }

    return result;
  },

  getFirstLineNumberForCode: function() {
    return this.hasParams() ? this.getBehaviorParams().length + 4 : 2;
  },

  getLastLineNumberForCode: function() {
    var numLines = this.getBehaviorNodeFunction().split('\n').length;
    return this.getFirstLineNumberForCode() + numLines;
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

  confirmAction: function(message, confirmCallback, cancelCallback) {
    var didConfirm = window.confirm(message);
    if (didConfirm && typeof(confirmCallback) === 'function') {
      confirmCallback.call(this);
    } else if (!didConfirm && typeof(cancelCallback) === 'function') {
      cancelCallback.call(this);
    }
  },

  undoChanges: function() {
    this.confirmAction("Are you sure you want to undo changes?", function() {
      this.setState({ behavior: this.getInitialState().behavior });
      this.toggleCodeEditor(this.shouldRevealCodeEditor());
    });
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
    this.setBehaviorProp('triggers', triggers);
  },

  focusOnParamIndex: function(index) {
    this.refs.codeHeader.focusIndex(index);
  },

  focusOnLastParam: function() {
    this.focusOnParamIndex(this.getBehaviorParams().length - 1)
  },

  onCodeChange: function(newCode) {
    this.setBehaviorProp('nodeFunction', newCode);
  },

  deleteCode: function() {
    this.confirmAction("Are you sure you want to clear the code?", function() {
      this.setBehaviorProp('params', []);
      this.setBehaviorProp('nodeFunction', '');
      this.toggleCodeEditor();
    });
  },

  onTemplateChange: function(newTemplateString) {
    var callback = function() {
      this.setState({ hasModifiedTemplate: true });
    };
    this.setBehaviorProp('responseTemplate', newTemplateString, callback)
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

  toggleTriggerHelp: function() {
    this.setState({
      triggerHelpVisible: !this.state.triggerHelpVisible
    });
  },

  toggleCodeEditor: function(forceState) {
    var newState = forceState !== undefined ? forceState : !this.state.revealCodeEditor;
    this.setState({
      revealCodeEditor: newState
    });
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
    this.setState({
      boilerplateHelpVisible: !this.state.boilerplateHelpVisible
    });
  },

  toggleEnvVariableExpansion: function() {
    this.setState({
      expandEnvVariables: !this.state.expandEnvVariables
    });
  },

  getResponseHeader: function() {
    return this.state.revealCodeEditor ? "Then respond with" : "Ellipsis will respond with";
  },

  hasUserParameters: function() {
    // TODO: when we have user parameters that aren't part of code, include those
    return this.hasParams();
  },

  templateUsesMarkdown: function() {
    var template = this.getBehaviorTemplate();
    /* Big ugly flaming pile of regex to try and guess at Markdown usage: */
    var matches = [
      '\\*.+?\\*', /* Bold/italics */
      '_.+?_', /* Bold/italics */
      '\\[.+?\\]\\(.+?\\)', /* Links */
      '(\\[.+?\\]){2}', /* Links by reference */
      '^.+\\n[=-]+', /* Underlined headers */
      '^#+\\s+.+', /* # Headers */
      '^\\d\\.\\s+.+', /* Numbered lists */
      '^\\*\\s+.+', /* Bulleted lists */
      '^>.+', /* Block quote */
      '`.+?`', /* Code */
      '```', /* Code block */
      '^\\s*[-\\*]\\s*[-\\*]\\s*[-\\*]' /* Horizontal rule */
    ];
    var matchRegExp = new RegExp( '(' + matches.join( ')|(' ) + ')' );
    return template && template.match(matchRegExp);
  },

  templateIncludesParam: function() {
    var template = this.getBehaviorTemplate();
    return template && template.match(/\{\S+?\}/);
  },

  getTemplateHelp: function() {
    if (this.state.revealCodeEditor) {
      return (
        <span checkedWhen={this.templateIncludesParam()}>
          <span>Use <code>{"{exampleParamName}"}</code> to show any user-supplied parameter, or </span>
          <span><code>{"{successResult}"}</code> to show the parameter provided to </span>
          <span><code>onSuccess</code> in your code.</span>
        </span>
      );
    } else if (this.hasUserParameters()) {
      return (
        <span checkedWhen={this.templateIncludesParam()}>
          Use <code>{"{exampleParamName}"}</code> to show any user-supplied parameter.
        </span>
      )
    } else {
      return (
        <span>Add code above if you want to collect user input before returning a response.</span>
      );
    }
  },

  getCodeAutocompletions: function() {
    var envVars = this.state.envVariableNames.map(function(name) {
      return 'ellipsis.env.' + name;
    });

    return this.getCodeFunctionParams().concat(envVars);
  },

  getCodeFunctionParams: function() {
    var userParams = this.getBehaviorProp('params').map(function(param) { return param.name; });
    return userParams.concat(["onSuccess", "onError", "ellipsis"]);
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

          <div className="columns">
            <div className="column column-one-quarter form-field-group mts">
              <BehaviorEditorSectionHeading>When someone says</BehaviorEditorSectionHeading>

              <BehaviorEditorChecklist disabledWhen={this.isExistingBehavior()}>
                <span checkedWhen={this.hasPrimaryTrigger()} hiddenWhen={this.isExistingBehavior()}>
                  Write a question or phrase people should use to trigger a response.
                </span>
                <span checkedWhen={this.hasMultipleTriggers()} hiddenWhen={this.isExistingBehavior() && this.hasMultipleTriggers()}>
                  You can add multiple triggers.
                </span>
                <span checkedWhen={this.triggersUseParams()}>
                  <span>A trigger can include “fill-in-the-blank” parts, e.g. <code className="plxs">{"Call me {name}"}</code></span>
                  <span className="pls"><BehaviorEditorHelpButton onClick={this.toggleTriggerHelp} /></span>
                </span>
              </BehaviorEditorChecklist>

            </div>
            <div className="column column-three-quarters pll form-field-group">
              <Collapsible revealWhen={this.state.triggerHelpVisible}>
                <BehaviorEditorTriggerHelp onCollapseClick={this.toggleTriggerHelp} />
              </Collapsible>

              <div className="mbm">
              {this.getBehaviorTriggers().map(function(trigger, index) {
                return (
                  <BehaviorEditorTriggerInput
                    className={index === 0 ? "form-input-large" : ""}
                    key={"BehaviorEditorTrigger" + index}
                    ref={"trigger" + index}
                    value={trigger}
                    hideDelete={index === 0}
                    onChange={this.onTriggerChange.bind(this, index)}
                    onDelete={this.deleteTriggerAtIndex.bind(this, index)}
                    onEnterKey={this.onTriggerEnterKey.bind(this, index)}
                  />
                );
              }, this)}
              </div>
              <div className="pr-symbol align-r">
                <button type="button" className="button-s" onClick={this.addTrigger}>Add another trigger</button>
              </div>
            </div>
          </div>

          <Collapsible revealWhen={this.state.revealCodeEditor}>
            <hr className="mtn" />
          </Collapsible>

          <Collapsible revealWhen={!this.state.revealCodeEditor}>
            <div className="box-help form-field-group">
            <div className="columns columns-elastic">
              <div className="column column-expand">
                <p className="mbn">
                  <span>You can run code to determine a result, with additional input from the user if needed, </span>
                  <span>or provide a simple response below.</span>
                </p>
              </div>
              <div className="column column-shrink align-m">
                <button type="button" className="button-s" onClick={this.toggleCodeEditor}>
                  Add code
                </button>
              </div>
            </div>
            </div>
          </Collapsible>

          <Collapsible revealWhen={this.state.revealCodeEditor} animationDuration={0.5}>
          <div className="columns">
            <div className="column column-one-quarter form-field-group">

              <BehaviorEditorSectionHeading>Ellipsis will do</BehaviorEditorSectionHeading>

              <BehaviorEditorChecklist disabledWhen={this.isExistingBehavior()}>
                <span checkedWhen={this.hasCode()} hiddenWhen={this.isExistingBehavior()}>
                  Write a node.js function to determine a result.
                </span>

                <span checkedWhen={this.hasCalledOnSuccess()} hiddenWhen={this.isExistingBehavior()}>
                  <span>Call <code>onSuccess</code> with a string or </span>
                  <span>object to include in the response.</span>
                </span>

                <span checkedWhen={this.hasParams()} hiddenWhen={this.isExistingBehavior() && this.hasParams()}>
                  <span>If you need more information from the user, add one or more parameters </span>
                  <span>to your function.</span>
                </span>
              </BehaviorEditorChecklist>

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

              <Collapsible revealWhen={this.state.boilerplateHelpVisible}>
                <BehaviorEditorBoilerplateParameterHelp
                  envVariableNames={this.state.envVariableNames}
                  onExpandToggle={this.toggleEnvVariableExpansion}
                  expandEnvVariables={this.state.expandEnvVariables}
                  onCollapseClick={this.toggleBoilerplateHelp}
                />
              </Collapsible>

              <div className="position-relative pr-symbol border-right">
                <BehaviorEditorCodeEditor
                  value={this.getBehaviorNodeFunction()}
                  onChange={this.onCodeChange}
                  firstLineNumber={this.getFirstLineNumberForCode()}
                  lineWrapping={this.state.codeEditorUseLineWrapping}
                  autocompletions={this.getCodeAutocompletions()}
                  functionParams={this.getCodeFunctionParams()}
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

              <BehaviorEditorCodeFooter
                lineNumber={this.getLastLineNumberForCode()}
                onCodeDelete={this.deleteCode}
              />

            </div>
          </div>

          <hr className="mtn" />
          </Collapsible>

          <div className="columns">

            <div className="column column-one-quarter mbxl">

              <BehaviorEditorSectionHeading>{this.getResponseHeader()}</BehaviorEditorSectionHeading>

              <BehaviorEditorChecklist disabledWhen={this.isExistingBehavior()}>
                <span checkedWhen={this.templateUsesMarkdown()}>
                  <span>Use <a href="http://commonmark.org/help/" target="_blank">Markdown</a> </span>
                  <span>to format the response, add links, etc.</span>
                </span>

                {this.getTemplateHelp()}
              </BehaviorEditorChecklist>

            </div>

            <div className="column column-three-quarters pll mbxl">
              <div className="position-relative CodeMirror-container-no-gutter">
                <Codemirror value={this.getBehaviorTemplate()}
                  onChange={this.onTemplateChange}
                  options={{
                    mode: {
                      name: "markdown",
                      /* Use CommonMark-appropriate settings */
                      fencedCodeBlocks: true,
                      underscoresBreakWords: false
                    },
                    gutters: ['CodeMirror-no-gutter'],
                    indentUnit: 4,
                    indentWithTabs: true,
                    lineWrapping: true,
                    lineNumbers: false,
                    smartIndent: true,
                    tabSize: 4,
                    viewportMargin: Infinity,
                    placeholder: "The result is {successResult}"
                  }}
                />
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
