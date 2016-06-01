define(function(require) {
var React = require('react'),
  ReactDOM = require('react-dom'),
  Codemirror = require('./react-codemirror'),
  CodemirrorJSMode = require('./codemirror/mode/javascript/javascript'),
  CodemirrorMarkdownMode = require('./codemirror/mode/markdown/markdown'),
  CodemirrorShowHint = require('./codemirror/addon/hint/show-hint'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorBoilerplateParameterHelp = require('./behavior_editor_boilerplate_parameter_help'),
  BehaviorEditorCodeHeader = require('./behavior_editor_code_header'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorHelpButton = require('./behavior_editor_help_button'),
  BehaviorEditorHiddenJsonInput = require('./behavior_editor_hidden_json_input'),
  BehaviorEditorInput = require('./behavior_editor_input'),
  BehaviorEditorSectionHeading = require('./behavior_editor_section_heading'),
  BehaviorEditorSettingsButton = require('./behavior_editor_settings_button'),
  BehaviorEditorSettingsMenu = require('./behavior_editor_settings_menu'),
  BehaviorEditorTriggerInput = require('./behavior_editor_trigger_input'),
  BehaviorEditorUserInputDefinition = require('./behavior_editor_user_input_definition'),
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

  isNewBehavior: function() {
    return !this.props.nodeFunction;
  },

  shouldRevealCodeEditor: function() {
    return this.props.shouldRevealCodeEditor || this.props.nodeFunction
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
    return "**The magic 8-ball says:**\n\n“" + responses[rand] + "”";
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
      magic8BallResponse: this.getMagic8BallResponse()
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
    return this.getBehaviorProp('responseTemplate') || this.getDefaultBehaviorTemplate();
  },

  getBehaviorTriggers: function() {
    return this.getBehaviorProp('triggers');
  },

  hasCalledOnSuccess: function() {
    var code = this.getBehaviorNodeFunction();
    return code && code.match(/\bonSuccess\(.+?\)/);
  },

  hasMultipleTriggers: function() {
    return this.getBehaviorTriggers().length > 1;
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

  toggleCodeEditor: function() {
    this.setState({
      revealCodeEditor: !this.state.revealCodeEditor
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

  getResponseHeader: function() {
    return this.state.revealCodeEditor ? "Then respond with" : "Ellipsis will respond with";
  },

  hasUserParameters: function() {
    // TODO: when we have user parameters that aren't part of code, include those
    return this.hasParams();
  },

  getTemplateHelp: function() {
    if (this.state.revealCodeEditor) {
      return (
        <span>
          <span>You can include any user-supplied parameters, plus the special </span>
          <span><code>{"{successResult}"}</code> variable.</span>
        </span>
      );
    } else if (this.hasUserParameters()) {
      return (
        <span>You can include any user-supplied parameters.</span>
      )
    } else {
      return (
        <span>Add code if you want to collect user input before returning a response.</span>
      );
    }
  },

  autocompleteParams: function(cm, options) {
    var matches = [];
    var possibleWords = ["onSuccess", "onError", "ellipsis"].concat(this.getBehaviorParams());
    this.state.envVariableNames.forEach(function(name) {
      possibleWords.push('ellipsis.env.' + name);
    });

    var cursor = cm.getCursor();
    var line = cm.getLine(cursor.line);
    var start = cursor.ch;
    var end = cursor.ch;

    while (start && /\w/.test(line.charAt(start - 1))) {
      --start;
    }
    while (end < line.length && /\w/.test(line.charAt(end))) {
      ++end;
    }

    var word = line.slice(start, end).toLowerCase();

    possibleWords.forEach(function(w) {
      if (w.indexOf(word) !== -1) {
        matches.push(w);
      }
    });

    return {
      list: matches,
      from: { line: cursor.line, ch: start },
      to: { line: cursor.line, ch: end }
    }
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

              <ul className="type-s list-space-s checklist">
                <li>Write a question or phrase people should use to trigger a response.</li>
                <li className={this.hasMultipleTriggers() ? "checklist-checked" : ""}>You can add multiple triggers.</li>
              </ul>

            </div>
            <div className="column column-three-quarters pll form-field-group">
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

          <div className={this.visibleWhen(this.state.revealCodeEditor, true)}>
            <hr className="mtn" />
          </div>

          <div className={this.visibleWhen(!this.state.revealCodeEditor, true)}>
            <div className="bg-blue-lighter border border-blue pal form-field-group">
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
          </div>

          <div className={"columns" + this.visibleWhen(this.state.revealCodeEditor, true)}>
            <div className="column column-one-quarter form-field-group">

              <BehaviorEditorSectionHeading>Ellipsis will do</BehaviorEditorSectionHeading>

              <ul className="type-s list-space-s checklist">
                <li className="">
                  <span>Write a node.js function to determine a result.</span>
                </li>

                <li className={this.hasCalledOnSuccess() ? "checklist-checked" : ""}>
                  <span>Call <code>onSuccess</code> with a string or </span>
                  <span>object to include in the response.</span>
                </li>

                <li className={this.hasParams() ? "checklist-checked" : ""}>
                  <span>If you need more information from the user, add one or more parameters </span>
                  <span>to your function.</span>
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
                  ref="nodeFunctionEditor"
                  onChange={this.onCodeChange}
                  options={{
                    mode: "javascript",
                    firstLineNumber: this.getFirstLineNumberForCode(),
                    hintOptions: { hint: this.autocompleteParams },
                    indentUnit: 2,
                    indentWithTabs: false,
                    lineWrapping: this.state.codeEditorUseLineWrapping,
                    lineNumbers: true,
                    smartIndent: true,
                    tabSize: 2,
                    viewportMargin: Infinity,
                    extraKeys: {
                      Esc: "autocomplete",
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

              <div className="border-left border-bottom border-right border-radius-bottom pvs">
                <div className="columns columns-elastic">
                  <div className="column column-shrink plxxxl prn align-r position-relative">
                    <code className="type-disabled type-s position-absolute position-top-right prxs">{this.getLastLineNumberForCode()}</code>
                  </div>
                  <div className="column column-expand plxs">
                    <code className="type-weak type-s">{"}"}</code>
                  </div>
                </div>
              </div>

            </div>
          </div>

          <div className={this.visibleWhen(this.state.revealCodeEditor, true)}>
            <hr className="mtn" />
          </div>

          <div className="columns">

            <div className="column column-one-quarter mbxl">

              <BehaviorEditorSectionHeading>{this.getResponseHeader()}</BehaviorEditorSectionHeading>

              <ul className="type-s list-space-s checklist">
                <li>
                  <span>Use <a href="http://commonmark.org/help/" target="_blank">Markdown</a> </span>
                  <span>to format the response, add links, etc.</span>
                </li>

                <li>
                  {this.getTemplateHelp()}
                </li>
              </ul>

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
