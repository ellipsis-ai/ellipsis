define(function(require) {
var React = require('react'),
  Codemirror = require('./react-codemirror'),
  CodemirrorMarkdownMode = require('codemirror/mode/markdown/markdown'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  BehaviorEditorBoilerplateParameterHelp = require('./behavior_editor_boilerplate_parameter_help'),
  BehaviorEditorChecklist = require('./behavior_editor_checklist'),
  BehaviorEditorCodeEditor = require('./behavior_editor_code_editor'),
  BehaviorEditorCodeFooter = require('./behavior_editor_code_footer'),
  BehaviorEditorCodeHeader = require('./behavior_editor_code_header'),
  BehaviorEditorConfirmActionPanel = require('./behavior_editor_confirm_action_panel'),
  BehaviorEditorDeleteButton = require('./behavior_editor_delete_button'),
  BehaviorEditorDropdownMenu = require('./behavior_editor_dropdown_menu'),
  BehaviorEditorHelpButton = require('./behavior_editor_help_button'),
  BehaviorEditorHiddenJsonInput = require('./behavior_editor_hidden_json_input'),
  BehaviorEditorInput = require('./behavior_editor_input'),
  BehaviorEditorSectionHeading = require('./behavior_editor_section_heading'),
  BehaviorEditorTriggerHelp = require('./behavior_editor_trigger_help'),
  BehaviorEditorTriggerOptionsHelp = require('./behavior_editor_trigger_options_help'),
  BehaviorEditorTriggerInput = require('./behavior_editor_trigger_input'),
  BehaviorEditorUserInputDefinition = require('./behavior_editor_user_input_definition'),
  BehaviorEditorVersionsPanel = require('./behavior_editor_versions_panel'),
  SVGSettingsIcon = require('./svg/settings'),
  Collapsible = require('./collapsible'),
  CsrfTokenHiddenInput = require('./csrf_token_hidden_input'),
  BrowserUtils = require('./browser_utils'),
  ImmutableObjectUtils = require('./immutable_object_utils');

return React.createClass({
  displayName: 'BehaviorEditor',
  mixins: [BehaviorEditorMixin],

  propTypes: {
    teamId: React.PropTypes.string.isRequired,
    behaviorId: React.PropTypes.string,
    functionBody: React.PropTypes.string,
    responseTemplate: React.PropTypes.string,
    params: React.PropTypes.arrayOf(React.PropTypes.shape({
      name: React.PropTypes.string.isRequired,
      question: React.PropTypes.string.isRequired
    })),
    triggers: React.PropTypes.arrayOf(React.PropTypes.shape({
      text: React.PropTypes.string.isRequired,
      requiresMention: React.PropTypes.bool,
      isRegex: React.PropTypes.bool,
      caseSensitive: React.PropTypes.bool
    })),
    csrfToken: React.PropTypes.string.isRequired,
    justSaved: React.PropTypes.bool,
    envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string),
    shouldRevealCodeEditor: React.PropTypes.bool
  },


  /* Getters */

  getActiveDropdown: function() {
    return this.state.activeDropdown && this.state.activeDropdown.name ? this.state.activeDropdown.name : "";
  },

  getActivePanel: function() {
    return this.state.activePanel && this.state.activePanel.name ? this.state.activePanel.name : "";
  },

  getBehaviorFunctionBody: function() {
    return this.getBehaviorProp('functionBody') || "";
  },

  getBehaviorParams: function() {
    return this.getBehaviorProp('params') || [];
  },

  getBehaviorProp: function(key) {
    return this.state.behavior[key];
  },

  getBehaviorStatusText: function() {
    if (this.state.justSaved) {
      return (<span className="type-green fade-in"> — saved successfully</span>);
    } else if (this.isModified()) {
      return (<span className="type-pink fade-in"> — unsaved changes</span>);
    } else {
      return (<span>&nbsp;</span>);
    }
  },

  getBehaviorTemplate: function() {
    var template = this.getBehaviorProp('responseTemplate');
    if (!template && !this.hasModifiedTemplate()) {
      return this.getDefaultBehaviorTemplate();
    } else {
      return template;
    }
  },

  getBehaviorTriggers: function() {
    return this.getBehaviorProp('triggers');
  },

  getCodeAutocompletions: function() {
    var envVars = this.state.envVariableNames.map(function(name) {
      return 'ellipsis.env.' + name;
    });

    return this.getCodeFunctionParams().concat(envVars);
  },

  getCodeEditorDropdownLabel: function() {
    return (<SVGSettingsIcon label="Editor settings" />);
  },

  getCodeFunctionParams: function() {
    var userParams = this.getBehaviorParams().map(function(param) { return param.name; });
    return userParams.concat(["onSuccess", "onError", "ellipsis"]);
  },

  getDefaultBehaviorTemplate: function() {
    if (this.hasCalledOnSuccess()) {
      return 'The answer is: {successResult}.';
    } else {
      return this.state.magic8BallResponse;
    }
  },

  getFirstLineNumberForCode: function() {
    return this.hasParams() ? this.getBehaviorParams().length + 4 : 2;
  },

  getInitialTriggers: function() {
    if (this.props.triggers && this.props.triggers.length > 0) {
      return this.props.triggers;
    } else {
      return [this.getNewBlankTrigger()];
    }
  },

  getIterationTemplateHelp: function() {
    return (
      <div checkedWhen={this.templateIncludesIteration()}>
        Iterating through a list:<br />
        <div className="box-code-example">
          {"{for item in successResult.items}"}<br />
          &nbsp;* {"{item}"}<br />
          {"{endfor}"}
        </div>
      </div>
    );
  },

  getLastLineNumberForCode: function() {
    var numLines = this.getBehaviorFunctionBody().split('\n').length;
    return this.getFirstLineNumberForCode() + numLines;
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

  getNewBlankTrigger: function() {
    return {
      text: "",
      requiresMention: false,
      isRegex: false,
      caseSensitive: false
    };
  },

  getPathTemplateHelp: function() {
    return (
      <span checkedWhen={this.templateIncludesPath()}>
        Properties of the result:<br />
        <div className="box-code-example">
          Name: {"{successResult.user.name}"}
        </div>
      </span>
    );
  },

  getResponseHeader: function() {
    return this.state.revealCodeEditor ? "Then respond with" : "Ellipsis will respond with";
  },

  getSuccessResultTemplateHelp: function() {
    return (
      <div checkedWhen={this.templateIncludesSuccessResult()}>
        The result provided to <code>onSuccess</code>:<br />
        <div className="box-code-example">
          The answer is {"{successResult}"}
        </div>
      </div>
    );
  },

  getTemplateDataHelp: function() {
    if (this.state.revealCodeEditor) {
      return (
        <div>
          <span>You can include data in your response.<br /></span>
          <BehaviorEditorChecklist className="mtxs" disabledWhen={this.isExistingBehavior()}>
            {this.getUserParamTemplateHelp()}
            {this.getSuccessResultTemplateHelp()}
            {this.getPathTemplateHelp()}
            {this.getIterationTemplateHelp()}
          </BehaviorEditorChecklist>
        </div>
      );
    }
  },

  getTimestampedBehavior: function(behavior) {
    return ImmutableObjectUtils.objectWithNewValueAtKey(behavior, 'createdAt', Date.now());
  },

  getUserParamTemplateHelp: function() {
    return (
      <div checkedWhen={this.templateIncludesParam()}>
        User-supplied parameters:<br />
        <div className="box-code-example">
        You said {this.hasParams() && this.getBehaviorParams()[0].name ?
          "{" + this.getBehaviorParams()[0].name + "}" :
          "{exampleParamName}"}
        </div>
      </div>
    );
  },

  getVersions: function() {
    return this.state.versions;
  },

  /* Setters/togglers */

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

  addTrigger: function() {
    this.setBehaviorProp('triggers', this.getBehaviorTriggers().concat(this.getNewBlankTrigger()), this.focusOnFirstBlankTrigger);
  },

  cancelVersionPanel: function() {
    this.hideActivePanel();
    this.showVersionIndex(0);
  },

  confirmDeleteBehavior: function() {
    this.setState({
      activePanel: { name: 'confirmDeleteBehavior', modal: true }
    });
  },

  confirmDeleteCode: function() {
    this.setState({
      activePanel: { name: 'confirmDeleteCode', modal: true }
    });
  },

  confirmUndo: function() {
    this.setState({
      activePanel: { name: 'confirmUndo', modal: true }
    });
  },

  deleteBehavior: function() {
    this.refs.deleteBehaviorForm.submit();
  },

  deleteCode: function() {
    this.setBehaviorProp('params', []);
    this.setBehaviorProp('functionBody', '');
    this.toggleCodeEditor();
    this.hideActivePanel();
  },

  deleteParamAtIndex: function(index) {
    this.setBehaviorProp('params', ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getBehaviorParams(), index));
  },

  deleteTriggerAtIndex: function(index) {
    var triggers = ImmutableObjectUtils.arrayRemoveElementAtIndex(this.getBehaviorTriggers(), index);
    this.setBehaviorProp('triggers', triggers);
  },

  loadVersions: function() {
    var url = '/version_info/' + encodeURIComponent(this.props.behaviorId);
    this.setState({
      versionsLoadStatus: 'loading'
    });
    fetch(url, { credentials: 'same-origin' })
      .then(function(response) {
        return response.json();
      }).then(function(json) {
        this.setState({
          versions: this.state.versions.concat(json),
          versionsLoadStatus: 'loaded'
        });
        this.refs.versionsPanel.reset();
      }.bind(this)).catch(function(ex) {
        // TODO: figure out what to do if there's a request error
        this.setState({
          versionsLoadStatus: 'error'
        });
      });
  },

  hideActiveDropdown: function() {
    this.setState({
      activeDropdown: null
    });
  },

  hideActivePanel: function() {
    this.setState({
      activePanel: null
    });
  },

  onDocumentClick: function(event) {
    this.hideActiveDropdown();
  },

  onSaveClick: function() {
    this.setState({
      isSaving: true
    });
  },

  showVersionIndex: function(versionIndex, optionalCallback) {
    var version = this.getVersions()[versionIndex];
    this.setState({
      behavior: {
        teamId: this.props.teamId,
        behaviorId: this.props.behaviorId,
        functionBody: version.functionBody,
        responseTemplate: version.responseTemplate,
        params: version.params,
        triggers: version.triggers
      }
    }, optionalCallback);
  },

  restoreVersionIndex: function(versionIndex) {
    this.showVersionIndex(versionIndex, function() {
      this.refs.behaviorForm.submit();
    });
  },

  setBehaviorProp: function(key, value, callback) {
    var newBehavior = ImmutableObjectUtils.objectWithNewValueAtKey(this.state.behavior, value, key);
    var timestampedBehavior = this.getTimestampedBehavior(newBehavior);
    var newVersions = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.versions, timestampedBehavior, 0);
    this.setState({
      behavior: newBehavior,
      versions: newVersions
    }, callback);
  },

  showVersions: function() {
    if (!this.versionsMaybeLoaded()) {
      this.loadVersions();
    }
    this.setState({
      activePanel: { name: 'versionHistory', modal: true }
    });
  },

  toggleActiveDropdown: function(name) {
    var alreadyOpen = this.getActiveDropdown() === name;
    this.setState({
      activeDropdown: alreadyOpen ? null : { name: name }
    });
  },

  toggleActivePanel: function(name, beModal) {
    var alreadyOpen = this.getActivePanel() === name;
    this.setState({
      activePanel: alreadyOpen ? null : { name: name, modal: !!beModal }
    });
  },

  toggleBoilerplateHelp: function() {
    this.toggleActivePanel('helpForBoilerplateParameters');
  },

  toggleCodeEditor: function() {
    this.setState({
      revealCodeEditor: !this.state.revealCodeEditor
    });
  },

  toggleCodeEditorLineWrapping: function() {
    this.setState({
      codeEditorUseLineWrapping: !this.state.codeEditorUseLineWrapping
    });
  },

  toggleEditorSettingsMenu: function() {
    this.toggleActiveDropdown('codeEditorSettings');
  },

  toggleEnvVariableExpansion: function() {
    this.setState({
      expandEnvVariables: !this.state.expandEnvVariables
    });
  },

  toggleManageBehaviorMenu: function() {
    this.toggleActiveDropdown('manageBehavior');
  },

  toggleTriggerHelp: function() {
    this.toggleActivePanel('helpForTriggerParameters');
  },

  toggleTriggerOptionsHelp: function() {
    this.toggleActivePanel('helpForTriggerOptions');
  },

  toggleVersionListMenu: function() {
    this.toggleActiveDropdown('versionList');
  },

  updateCode: function(newCode) {
    this.setBehaviorProp('functionBody', newCode);
  },

  updateParamAtIndexWithParam: function(index, newParam) {
    this.setBehaviorProp('params', ImmutableObjectUtils.arrayWithNewElementAtIndex(this.getBehaviorParams(), newParam, index));
  },

  updateTemplate: function(newTemplateString) {
    var callback = function() {
      this.setState({ hasModifiedTemplate: true });
    };
    this.setBehaviorProp('responseTemplate', newTemplateString, callback)
  },

  updateTriggerAtIndexWithTrigger: function(index, newTrigger) {
    this.setBehaviorProp('triggers', ImmutableObjectUtils.arrayWithNewElementAtIndex(this.getBehaviorTriggers(), newTrigger, index));
  },

  undoChanges: function() {
    var newBehavior = this.getInitialState().behavior;
    var timestampedBehavior = this.getTimestampedBehavior(newBehavior);
    var newVersions = ImmutableObjectUtils.arrayWithNewElementAtIndex(this.state.versions, timestampedBehavior, 0);
    this.setState({
      behavior: newBehavior,
      versions: newVersions,
      revealCodeEditor: this.shouldRevealCodeEditor()
    });
    this.hideActivePanel();
  },


  /* Booleans */

  hasCalledOnSuccess: function() {
    var code = this.getBehaviorFunctionBody();
    var success = code && code.match(/\bonSuccess\([\s\S]+?\)/);
    return success;
  },

  hasCode: function() {
    return this.getBehaviorFunctionBody().match(/\S/);
  },

  hasModalPanel: function() {
    return !!(this.state.activePanel && this.state.activePanel.modal);
  },

  hasModifiedTemplate: function() {
    return this.state.hasModifiedTemplate;
  },

  hasMultipleTriggers: function() {
    return this.getBehaviorTriggers().length > 1;
  },

  hasParams: function() {
    return this.getBehaviorParams() && this.getBehaviorParams().length > 0;
  },

  hasPrimaryTrigger: function() {
    var triggers = this.getBehaviorTriggers();
    return triggers.length > 0 && triggers[0];
  },

  hasUserParameters: function() {
    // TODO: when we have user parameters that aren't part of code, include those
    return this.hasParams();
  },

  isExistingBehavior: function() {
    return !!(this.props.functionBody || this.props.responseTemplate);
  },

  isModified: function() {
    var currentMatchesInitial = JSON.stringify(this.state.behavior) !== JSON.stringify(this.getInitialState().behavior);
    var previewingVersions = this.getActivePanel() === 'versionHistory';
    return currentMatchesInitial && !previewingVersions;
  },

  shouldFilterCurrentVersion: function() {
    var firstTwoVersions = this.getVersions().slice(0, 2);
    return firstTwoVersions.length === 2 && this.versionEqualsVersion(firstTwoVersions[0], firstTwoVersions[1]);
  },

  shouldRevealCodeEditor: function() {
    return !!(this.props.shouldRevealCodeEditor || this.props.functionBody)
  },

  templateIncludesIteration: function() {
    var template = this.getBehaviorTemplate();
    return template && template.match(/\{endfor\}/);
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

  templateIncludesPath: function() {
    var template = this.getBehaviorTemplate();
    return template && template.match(/\{(\S+\.\S+)+?\}/);
  },

  templateIncludesSuccessResult: function() {
    var template = this.getBehaviorTemplate();
    return template && template.match(/\{successResult.*?\}/);
  },

  triggersUseParams: function() {
    return this.getBehaviorTriggers().some(function(trigger) {
      return trigger.text.match(/{.+}/);
    });
  },

  versionEqualsVersion: function(version1, version2) {
    var shallow1 = JSON.stringify({
      functionBody: version1.functionBody,
      responseTemplate: version1.responseTemplate,
      params: version1.params,
      triggers: version1.triggers
    });
    var shallow2 = JSON.stringify({
      functionBody: version2.functionBody,
      responseTemplate: version2.responseTemplate,
      params: version2.params,
      triggers: version2.triggers
    });
    return shallow1 === shallow2;
  },

  versionsMaybeLoaded: function() {
    return this.state.versionsLoadStatus === 'loading' || this.state.versionsLoadStatus === 'loaded';
  },

  /* Interaction and event handling */

  ensureCursorVisible: function(editor) {
    if (!this.refs.footer) {
      return;
    }
    var cursorBottom = editor.cursorCoords(false).bottom;
    BrowserUtils.ensureYPosInView(cursorBottom, this.refs.footer.clientHeight);
  },

  focusOnFirstBlankTrigger: function() {
    var blankTrigger = Object.keys(this.refs).find(function(key) {
      return key.match(/^trigger\d+$/) && this.refs[key].isEmpty();
    }, this);
    if (blankTrigger) {
      this.refs[blankTrigger].focus();
    }
  },

  focusOnParamIndex: function(index) {
    this.refs.codeHeader.focusIndex(index);
  },

  focusOnLastParam: function() {
    this.focusOnParamIndex(this.getBehaviorParams().length - 1)
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

  componentDidMount: function() {
    window.document.addEventListener('click', this.onDocumentClick, false);
  },

  /* Component API methods */
  componentDidUpdate: function() {
    // Note that calling setState on every update triggers an infinite loop
    if (this.state.justSaved) {
      this.setState({ justSaved: false });
      BrowserUtils.removeQueryParam('justSaved');
    }
  },

  getInitialState: function() {
    var initialBehavior = {
      teamId: this.props.teamId,
      behaviorId: this.props.behaviorId,
      functionBody: this.props.functionBody,
      responseTemplate: this.props.responseTemplate,
      params: this.props.params,
      triggers: this.getInitialTriggers()
    };
    return {
      behavior: initialBehavior,
      activeDropdown: null,
      activePanel: null,
      codeEditorUseLineWrapping: false,
      expandEnvVariables: false,
      justSaved: this.props.justSaved,
      isSaving: false,
      envVariableNames: this.props.envVariableNames || [],
      revealCodeEditor: this.shouldRevealCodeEditor(),
      magic8BallResponse: this.getMagic8BallResponse(),
      hasModifiedTemplate: !!this.props.responseTemplate,
      versions: [this.getTimestampedBehavior(initialBehavior)],
      versionsLoadStatus: null
    };
  },

  render: function() {
    return (

      <div>

      <div className="bg-light">
        <div className="container pbm">
          <div className="columns columns-elastic">
            <div className="column column-expand">
              <h3 className="mbn type-weak">
                <span>Edit behavior</span>
                <span className="type-italic">{this.getBehaviorStatusText()}</span>
              </h3>
            </div>

            {/*
             <form ref="testBehaviorForm" action="/test_behavior_version" method="POST">
               <CsrfTokenHiddenInput value={this.props.csrfToken} />
               <input type="text" name="message" />
               <input type="hidden" name="behaviorId" value={this.props.behaviorId} />
               <input type="submit" />
             </form>
            */}

            <div className="column column-shrink ptl align-r">
              <BehaviorEditorDropdownMenu
                openWhen={this.getActiveDropdown() === 'manageBehavior'}
                label="Manage behavior"
                menuClassName="popup-dropdown-menu-right"
                toggle={this.toggleManageBehaviorMenu}
              >
                <BehaviorEditorDropdownMenu.Item onClick={this.showVersions} label="View/restore previous versions" />
                <BehaviorEditorDropdownMenu.Item onClick={this.confirmDeleteBehavior} label="Delete behavior" />
              </BehaviorEditorDropdownMenu>
            </div>
          </div>
        </div>
      </div>

      <form action="/save_behavior" method="POST" ref="behaviorForm">

        <CsrfTokenHiddenInput
          value={this.props.csrfToken}
          />
        <BehaviorEditorHiddenJsonInput
          value={JSON.stringify(this.state.behavior)}
        />

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
                  <span className="pls">
                    <BehaviorEditorHelpButton onClick={this.toggleTriggerHelp} toggled={this.getActivePanel() === 'helpForTriggerParameters'} />
                  </span>
                </span>
              </BehaviorEditorChecklist>

            </div>
            <div className="column column-three-quarters pll form-field-group">
              <div className="mbm">
              {this.getBehaviorTriggers().map(function(trigger, index) {
                return (
                  <BehaviorEditorTriggerInput
                    className={index === 0 ? "form-input-large" : ""}
                    includeHelp={index === 0}
                    key={"BehaviorEditorTrigger" + index}
                    id={"trigger" + index}
                    ref={"trigger" + index}
                    value={trigger.text}
                    requiresMention={trigger.requiresMention}
                    isRegex={trigger.isRegex}
                    caseSensitive={trigger.caseSensitive}
                    hideDelete={!this.hasMultipleTriggers()}
                    onChange={this.updateTriggerAtIndexWithTrigger.bind(this, index)}
                    onDelete={this.deleteTriggerAtIndex.bind(this, index)}
                    onEnterKey={this.onTriggerEnterKey.bind(this, index)}
                    onHelpClick={this.toggleTriggerOptionsHelp}
                    helpVisible={this.getActivePanel() === 'helpForTriggerOptions'}
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
            <div className="box-help border form-field-group">
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
                  <span>Call <code>onSuccess</code> with a string, object, or array </span>
                  <span>to include in the response below.</span>
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
                onParamChange={this.updateParamAtIndexWithParam}
                onParamDelete={this.deleteParamAtIndex}
                onParamAdd={this.addParam}
                onEnterKey={this.onParamEnterKey}
                helpVisible={this.getActivePanel() === 'helpForBoilerplateParameters'}
                onToggleHelp={this.toggleBoilerplateHelp}
              />

              <div className="position-relative pr-symbol border-right">
                <BehaviorEditorCodeEditor
                  value={this.getBehaviorFunctionBody()}
                  onChange={this.updateCode}
                  onCursorChange={this.ensureCursorVisible}
                  firstLineNumber={this.getFirstLineNumberForCode()}
                  lineWrapping={this.state.codeEditorUseLineWrapping}
                  autocompletions={this.getCodeAutocompletions()}
                  functionParams={this.getCodeFunctionParams()}
                />
                <div className="position-absolute position-top-right">
                  <BehaviorEditorDropdownMenu
                    openWhen={this.getActiveDropdown() === 'codeEditorSettings'}
                    label={this.getCodeEditorDropdownLabel()}
                    labelClassName="button-dropdown-trigger-symbol"
                    menuClassName="popup-dropdown-menu-right"
                    toggle={this.toggleEditorSettingsMenu}
                  >
                    <BehaviorEditorDropdownMenu.Item
                      onClick={this.toggleCodeEditorLineWrapping}
                      checkedWhen={this.state.codeEditorUseLineWrapping}
                      label="Enable line wrap"
                    />
                  </BehaviorEditorDropdownMenu>
                </div>
              </div>

              <BehaviorEditorCodeFooter
                lineNumber={this.getLastLineNumberForCode()}
                onCodeDelete={this.confirmDeleteCode}
              />

            </div>
          </div>

          <hr className="mtn" />
          </Collapsible>

          <div className="columns">

            <div className="column column-one-quarter mbxl type-s">

              <BehaviorEditorSectionHeading>{this.getResponseHeader()}</BehaviorEditorSectionHeading>

              <BehaviorEditorChecklist disabledWhen={this.isExistingBehavior()}>
                <span checkedWhen={this.templateUsesMarkdown()}>
                  <span>Use <a href="http://commonmark.org/help/" target="_blank">Markdown</a> </span>
                  <span>to format the response, add links, etc.</span>
                </span>
                {this.state.revealCodeEditor ? "" : (
                  <span>Add code above if you want to collect user input before returning a response.</span>
                )}
              </BehaviorEditorChecklist>

              {this.getTemplateDataHelp()}
            </div>

            <div className="column column-three-quarters pll mbxl">
              <div className="position-relative CodeMirror-container-no-gutter">
                <Codemirror value={this.getBehaviorTemplate()}
                  onChange={this.updateTemplate}
                  onCursorChange={this.ensureCursorVisible}
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

        <div className={"bg-scrim position-z-almost-front position-fixed-full " + (this.hasModalPanel() ? "fade-in" : "display-none")}></div>
        <footer ref="footer" className={"position-fixed-bottom position-z-front border-top " +
          (this.isModified() ? "bg-white" : "bg-light-translucent")}
        >
          <Collapsible revealWhen={this.getActivePanel() === 'confirmUndo'}>
            <BehaviorEditorConfirmActionPanel confirmText="Undo changes" onConfirmClick={this.undoChanges} onCancelClick={this.hideActivePanel}>
              <p>This will undo any changes you’ve made since last saving. Are you sure you want to do this?</p>
            </BehaviorEditorConfirmActionPanel>
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'confirmDeleteBehavior'}>
            <BehaviorEditorConfirmActionPanel confirmText="Delete" onConfirmClick={this.deleteBehavior} onCancelClick={this.hideActivePanel}>
              <p>Are you sure you want to delete this behavior?</p>
            </BehaviorEditorConfirmActionPanel>
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'confirmDeleteCode'}>
            <BehaviorEditorConfirmActionPanel confirmText="Remove" onConfirmClick={this.deleteCode} onCancelClick={this.hideActivePanel}>
              <p>Are you sure you want to remove all of the code?</p>
            </BehaviorEditorConfirmActionPanel>
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'helpForTriggerParameters'}>
            <BehaviorEditorTriggerHelp onCollapseClick={this.toggleTriggerHelp} />
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'helpForTriggerOptions'}>
            <BehaviorEditorTriggerOptionsHelp onCollapseClick={this.toggleTriggerOptionsHelp} />
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'helpForBoilerplateParameters'}>
            <BehaviorEditorBoilerplateParameterHelp
              envVariableNames={this.state.envVariableNames}
              onExpandToggle={this.toggleEnvVariableExpansion}
              expandEnvVariables={this.state.expandEnvVariables}
              onCollapseClick={this.toggleBoilerplateHelp}
            />
          </Collapsible>

          <Collapsible revealWhen={this.getActivePanel() === 'versionHistory'}>
            <BehaviorEditorVersionsPanel
              ref="versionsPanel"
              menuToggle={this.toggleVersionListMenu}
              onCancelClick={this.cancelVersionPanel}
              onRestoreClick={this.restoreVersionIndex}
              onSwitchVersions={this.showVersionIndex}
              openMenuWhen={this.getActiveDropdown() === 'versionList'}
              shouldFilterCurrentVersion={this.shouldFilterCurrentVersion()}
              versions={this.getVersions()}
            />
          </Collapsible>

          <Collapsible revealWhen={!this.hasModalPanel()}>
            <div className="container pvm">
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
              <button type="button" disabled={!this.isModified()} onClick={this.confirmUndo}>Undo changes</button>
            </div>
          </Collapsible>

        </footer>

      </form>
      <form ref="deleteBehaviorForm" action="/delete_behavior" method="POST">
        <CsrfTokenHiddenInput value={this.props.csrfToken} />
        <input type="hidden" name="behaviorId" value={this.props.behaviorId} />
      </form>

      </div>
    );
  }
});

});
