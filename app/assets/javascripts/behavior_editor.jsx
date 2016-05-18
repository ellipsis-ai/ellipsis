define('behavior_editor', function(require) {

var React = require('react');
var ReactDOM = require('react-dom');
var Codemirror = require('react-codemirror');
require('codemirror/mode/javascript/javascript');

var BehaviorEditor = React.createClass({
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
      data: {
        behaviorId: this.props.behaviorId,
        description: this.props.description,
        nodeFunction: this.props.nodeFunction,
        params: this.props.params,
        triggers: this.props.triggers.concat(['']) // always add one blank trigger
      }
    };
  },

  getData: function(key) {
    return this.state.data[key];
  },

  setData: function(key, value, callback) {
    var newData = this.utils.objectWithNewValueAtKey(this.state.data, value, key);
    this.setState({ data: newData }, callback);
  },

  isModified: function() {
    return JSON.stringify(this.state.data) !== JSON.stringify(this.getInitialState().data);
  },

  undoChanges: function() {
    if (window.confirm("Are you sure you want to undo changes?")) {
      this.setState({ data: this.getInitialState().data });
    }
  },

  addTrigger: function() {
    this.setData('triggers', this.getData('triggers').concat(['']), this.focusOnFirstBlankTrigger);
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
    var triggers = this.utils.arrayRemoveElementAtIndex(this.getData('triggers'), index);
    if (index == triggers.length) {
      // Add a blank trigger on the end if the user deleted the last trigger
      triggers = triggers.concat(['']);
    }
    this.setData('triggers', triggers);
  },

  focusOnLastParam: function() {
    this.refs['param' + (this.getData('params').length - 1)].focus();
  },

  onDescriptionChange: function(newDescription) {
    this.setData('description', newDescription);
  },

  onCodeChange: function(newCode) {
    this.setData('nodeFunction', newCode);
  },

  addParam: function() {
    var newParamIndex = this.getData('params').length + 1;
    while (this.getData('params').some(function(param) {
      return param.name == 'userInput' + newParamIndex;
    })) {
      newParamIndex++;
    }
    var newParams = this.getData('params').concat([{ name: 'userInput' + newParamIndex, question: '' }]);
    this.setData('params', newParams, this.focusOnLastParam);
  },

  replaceParamAtIndexWithParam: function(index, newParam) {
    this.setData('params', this.utils.arrayWithNewElementAtIndex(this.getData('params'), newParam, index));
  },

  deleteParamAtIndex: function(index) {
    this.setData('params', this.utils.arrayRemoveElementAtIndex(this.getData('params'), index));
  },

  onParamEnterKey: function(index) {
    if (index + 1 < this.getData('params').length) {
      this.refs['param' + (index + 1)].focus();
    } else if (this.getData('params')[index].question != '') {
      this.addParam();
    }
  },

  onTriggerEnterKey: function(index) {
    if (index + 1 < this.getData('triggers').length) {
      this.refs['trigger' + (index + 1)].focus();
    } else if (this.getData('triggers')[index] != '') {
      this.addTrigger();
    }
  },

  onTriggerChange: function(index, newTrigger) {
    this.setData('triggers', this.utils.arrayWithNewElementAtIndex(this.getData('triggers'), newTrigger, index));
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
          value={JSON.stringify(this.state.data)}
        />
        <div className="form-field-group">
          <h3 className="mtxxxl mbn type-weak">
            <span>
              Edit behavior
            </span> <span className={"type-italic type-pink visibility " + (this.isModified() ? "visibility-visible" : "visibility-hidden")}>— unsaved changes</span>
          </h3>
          <BehaviorEditorInput
            className="form-input-borderless form-input-h2"
            placeholder="Describe the behavior in one phrase"
            value={this.getData('description')}
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
              {this.getData('params').map(function(param, index) {
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
            <div className="mll">
              <Codemirror value={this.getData('nodeFunction')}
                onChange={this.onCodeChange}
                options={{
                  mode: "javascript",
                  lineWrapping: true,
                  viewportMargin: Infinity
                }}
              />
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
          {this.getData('triggers').map(function(trigger, index) {
            return (
              <BehaviorEditorTriggerInput
                key={"BehaviorEditorTrigger" + index}
                ref={"trigger" + index}
                value={trigger}
                onChange={this.onTriggerChange.bind(this, index)}
                onDelete={this.deleteTriggerAtIndex.bind(this, index)}
                onEnterKey={this.onTriggerEnterKey.bind(this, index)}
                mayHideDelete={index + 1 == this.getData('triggers').length}
              />
            );
          }, this)}
          </div>
          <button type="button" onClick={this.addTrigger}>Add another trigger</button>
        </div>

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
                <span className="button-normal-label">Save and return</span>
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
            <div className="column column-expand prxs">
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
  onClick: function(event) {
    this.props.onClick();
    this.refs.button.blur();
  },

  render: function() {
    return (
      <button type="button"
        ref="button"
        className={"button-subtle button-shrink visibility " + (this.props.hidden ? "visibility-hidden" : "visibility-visible")}
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
      </button>
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
        <div className="column column-expand prxs">
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

return {
  load: function(data, containerId) {
    var myBehaviorEditor = React.createElement(BehaviorEditor, data);
    ReactDOM.render(myBehaviorEditor, document.getElementById(containerId));
  }
};

});
