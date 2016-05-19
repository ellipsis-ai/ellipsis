define('behavior_editor', [
  'react',
  'react-dom',
  './react-codemirror',
  './codemirror/mode/javascript/javascript'
], function(React, ReactDOM, Codemirror) {

var BehaviorEditorMixin = {
  visibleWhen: function(condition) {
    return " visibility " + (condition ? "visibility-visible" : "visibility-hidden") + " ";
  }
};

var BehaviorEditor = React.createClass({
  mixins: [BehaviorEditorMixin],

  propTypes: {
    behaviorId: React.PropTypes.string,
    description: React.PropTypes.string,
    nodeFunction: React.PropTypes.string,
    params: React.PropTypes.arrayOf(React.PropTypes.shape({
      name: React.PropTypes.string.isRequired,
      question: React.PropTypes.string.isRequired
    })),
    triggers: React.PropTypes.arrayOf(React.PropTypes.string)
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
      settingsMenuVisible: false
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

  onSaveClick: function() {
    this.setState({
      isSaving: true
    });
  },

  render: function() {
    return (
      <form action="/save_behavior" method="POST">
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
                <code className="type-weak type-s">onSuccess,<br />onError,<br />context</code>
              </div>
              <div className="column column-three-quarters ptxl align-c">
                <button type="button" onClick={this.addParam}>Add parameter</button>
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

var BehaviorEditorUserInputDefinition = React.createClass({
  onNameChange: function(newName) {
    this.props.onChange({ name: newName, question: this.props.question });
  },

  onQuestionChange: function(newQuestion) {
    this.props.onChange({ name: this.props.name, question: newQuestion });
  },

  onDeleteClick: function() {
    this.props.onDelete();
  },

  focus: function() {
    this.refs.name.focus();
    this.refs.name.select();
  },

  render: function() {
    return (
      <div className={"columns " + (this.props.hasMargin ? "mts" : "")}>
        <div className="column column-one-quarter">
          <div className="columns columns-elastic">
            <div className="column column-expand prs">
              <BehaviorEditorInput
                ref="name"
                className="form-input-borderless type-monospace type-s"
                placeholder="userInput"
                value={this.props.name}
                onChange={this.onNameChange}
              />
            </div>
            <div className="column column-shrink align-b">
              <code>,</code>
            </div>
          </div>
        </div>
        <div className="column column-three-quarters">
          <div className="columns columns-elastic">
            <div className="column column-expand prs">
              <div className="form-field-with-prefix">
                <label className="form-input-prefix"
                  htmlFor={"question" + this.props.id}
                  title="Write a question for @ellipsis to ask the user to provide this parameter."
                >Q:</label>
                <BehaviorEditorInput
                  id={"question" + this.props.id}
                  ref="question"
                  placeholder="Write a question to ask the user for this parameter"
                  autoFocus={this.props.shouldGrabFocus}
                  value={this.props.question}
                  onChange={this.onQuestionChange}
                  onEnterKey={this.props.onEnterKey}
                />
              </div>
            </div>
            <div className="column column-shrink">
              <BehaviorEditorDeleteButton
                onClick={this.onDeleteClick}
                title={"Delete the “" + this.props.name + "” parameter"}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }
});

var BehaviorEditorInput = React.createClass({
  onChange: function() {
    this.props.onChange(this.refs.input.value);
  },

  handleEnterKey: function(event) {
    if (event.which == 13) {
      event.preventDefault();
      if (typeof this.props.onEnterKey == 'function') {
        this.props.onEnterKey();
      }
    }
  },

  isEmpty: function() {
    return !this.refs.input.value;
  },

  focus: function() {
    this.refs.input.focus();
  },

  select: function() {
    this.refs.input.select();
  },

  render: function() {
    return (
      <input type="text"
        className={"form-input " + this.props.className}
        ref="input"
        id={this.props.id}
        value={this.props.value}
        placeholder={this.props.placeholder}
        autoFocus={this.props.autoFocus}
        onChange={this.onChange}
        onKeyPress={this.handleEnterKey}
      />
    );
  }
});

var BehaviorEditorHiddenJsonInput = React.createClass({
  render: function() {
    return (
      <input type="hidden" name="dataJson" value={this.props.value}/>
    );
  }
});

var BehaviorEditorDeleteButton = React.createClass({
  mixins: [BehaviorEditorMixin],
  onClick: function(event) {
    this.props.onClick();
    this.refs.button.blur();
  },

  render: function() {
    return (
      <span className="type-weak"><button type="button"
        ref="button"
        className={"button-subtle button-symbol" + this.visibleWhen(!this.props.hidden)}
        onMouseUp={this.onClick}
        title={this.props.title || "Delete"}
      >
        <svg role="img" aria-label="Delete" width="17px" height="24px" viewBox="0 0 17 24">
          <title>Delete</title>
          <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
            <g id="delete" fill="currentColor">
              <polygon
                points="3.356 19.968 8.504 14.784 13.652 19.968 16.28 17.304 11.168 12.156 16.28 6.972 13.652 4.308 8.504 9.492 3.356 4.308 0.728 6.972 5.84 12.156 0.728 17.304">
              </polygon>
            </g>
          </g>
        </svg>
      </button></span>
    );
  }
});

var BehaviorEditorTriggerInput = React.createClass({
  isEmpty: function() {
    return !this.props.value;
  },

  focus: function() {
    this.refs.input.focus();
  },

  render: function() {
    return (
      <div className="columns columns-elastic">
        <div className="column column-expand prs">
          <BehaviorEditorInput
            ref="input"
            value={this.props.value}
            placeholder="Add a trigger phrase or regular expression"
            onChange={this.props.onChange}
            onEnterKey={this.props.onEnterKey}
          />
        </div>
        <div className="column column-shrink">
          <BehaviorEditorDeleteButton
            onClick={this.props.onDelete}
            hidden={this.isEmpty() && this.props.mayHideDelete}
          />
        </div>
      </div>
    );
  }
});

var BehaviorEditorSettingsButton = React.createClass({
  onMouseDown: function() {
    this.refs.button.blur();
    this.props.onClick();
  },

  render: function() {
    return (
      <span className="type-weak">
        <button type="button"
          ref="button"
          className="button-invisible button-shrink button-dropdown-trigger"
          onMouseDown={this.onMouseDown}
        >
          <svg aria-label="Settings" width="21px" height="24px" viewBox="0 0 21 24">
            <title>Settings</title>
            <g id="Page-1" stroke="none" stroke-width="1" fill="none" fill-rule="evenodd">
              <g id="cog" fill="currentColor">
                <path d="M4.92382812,15.234375 C4.53710744,14.624997 4.24414162,13.9687535 4.04492188,13.265625 L0.65234375,13.265625 L0.65234375,9.9609375 L4.00976562,9.9609375 C4.10351609,9.59765443 4.22070242,9.24902511 4.36132812,8.91503906 C4.50195383,8.58105302 4.671874,8.26172027 4.87109375,7.95703125 L2.56835938,5.65429688 L4.90625,3.31640625 L7.24414062,5.671875 C7.53711084,5.50781168 7.84179529,5.36132877 8.15820312,5.23242188 C8.47461096,5.10351498 8.80273268,5.0039066 9.14257812,4.93359375 L9.14257812,1.734375 L12.4472656,1.734375 L12.4472656,5.00390625 C12.7871111,5.09765672 13.1093735,5.21777271 13.4140625,5.36425781 C13.7187515,5.51074292 14.0117173,5.67773344 14.2929688,5.86523438 L16.6484375,3.50976562 L18.9863281,5.84765625 L16.5605469,8.2734375 C16.7011726,8.5429701 16.8271479,8.81542831 16.9384766,9.09082031 C17.0498052,9.36621231 17.1406246,9.65624848 17.2109375,9.9609375 L20.3046875,9.9609375 L20.3046875,13.265625 L17.1757812,13.265625 C17.1054684,13.570314 17.0117193,13.8574205 16.8945312,14.1269531 C16.7773432,14.3964857 16.6425789,14.660155 16.4902344,14.9179687 L18.8105469,17.2207031 L16.4726562,19.5585938 L14.2050781,17.2910156 C13.6542941,17.6425799 13.0683625,17.9062491 12.4472656,18.0820312 L12.4472656,21.3867188 L9.14257812,21.3867188 L9.14257812,18.1699219 C8.48632484,18.0175774 7.88867457,17.789064 7.34960938,17.484375 L5.08203125,19.7519531 L2.74414062,17.4140625 L4.92382812,15.234375 Z M8.12304688,11.6132812 C8.12304688,11.9882831 8.19335867,12.3369125 8.33398438,12.6591797 C8.47461008,12.9814469 8.66503786,13.2626941 8.90527344,13.5029297 C9.14550901,13.7431653 9.4267562,13.933593 9.74902344,14.0742188 C10.0712907,14.2148445 10.41992,14.2851562 10.7949219,14.2851562 C11.1582049,14.2851562 11.5039046,14.2148445 11.8320312,14.0742188 C12.1601579,13.933593 12.4443347,13.7431653 12.6845703,13.5029297 C12.9248059,13.2626941 13.1152337,12.9814469 13.2558594,12.6591797 C13.3964851,12.3369125 13.4667969,11.9882831 13.4667969,11.6132812 C13.4667969,11.2499982 13.3964851,10.9042985 13.2558594,10.5761719 C13.1152337,10.2480452 12.9248059,9.96386839 12.6845703,9.72363281 C12.4443347,9.48339724 12.1601579,9.29296945 11.8320312,9.15234375 C11.5039046,9.01171805 11.1582049,8.94140625 10.7949219,8.94140625 C10.41992,8.94140625 10.0712907,9.01171805 9.74902344,9.15234375 C9.4267562,9.29296945 9.14550901,9.48339724 8.90527344,9.72363281 C8.66503786,9.96386839 8.47461008,10.2480452 8.33398438,10.5761719 C8.19335867,10.9042985 8.12304688,11.2499982 8.12304688,11.6132812 L8.12304688,11.6132812 Z"></path>
              </g>
            </g>
          </svg>
        </button>
      </span>
    );
  }
});

var BehaviorEditorSettingsMenu = React.createClass({
  mixins: [BehaviorEditorMixin],
  render: function() {
    return (
      <div className="position-relative">
        <ul className={"dropdown-menu dropdown-menu-right" + this.visibleWhen(this.props.isVisible)}>
          {React.Children.map(this.props.children, function(child) {
            return (<li onMouseUp={this.props.onItemClick}>{child}</li>);
          }, this)}
        </ul>
      </div>
    );
  }
});

return {
  load: function(data, containerId) {
    var myBehaviorEditor = React.createElement(BehaviorEditor, data);
    ReactDOM.render(myBehaviorEditor, document.getElementById(containerId));
  }
};

});
