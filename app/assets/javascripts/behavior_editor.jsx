var BehaviorEditorUtils = {
  arrayWithNewElementAtIndex: function(array, newElement, index) {
    // Create a copy of the old array with the indexed element replaced
    var newArray = array.slice();
    newArray[index] = newElement;
    return newArray;
  }
};

var BehaviorEditor = React.createClass({
  propTypes: {
    description: React.PropTypes.string,
    nodeFunction: React.PropTypes.string,
    params: React.PropTypes.arrayOf(React.PropTypes.shape({
      name: React.PropTypes.string.isRequired,
      question: React.PropTypes.string.isRequired
    })),
    triggers: React.PropTypes.arrayOf(React.PropTypes.string),
    regexTrigger: React.PropTypes.string
  },

  getInitialState: function() {
    return {
      description: this.props.description,
      nodeFunction: this.props.nodeFunction,
      params: this.props.params,
      questionFocusIndex: null,
      triggers: this.props.triggers,
      regexTrigger: this.props.regexTrigger
    };
  },

  addMoreTriggers: function() {
    this.setState({
      triggers: this.state.triggers.concat(['', '', ''])
    }, this.focusOnFirstBlankTrigger);
  },

  focusOnFirstBlankTrigger: function() {
    var blankTrigger = Object.keys(this.refs).find(function(key) {
      return key.match(/^trigger\d+$/) && this.refs[key].isEmpty();
    }, this);
    if (blankTrigger) {
      this.refs[blankTrigger].focus();
    }
  },

  focusOnLastParam: function() {
    this.refs['param' + (this.state.params.length - 1)].focus();
  },

  onDescriptionChange: function(newDescription) {
    this.setState({ description: newDescription });
  },

  getCodeName: function() {
    var stripped = this.state.description.toLowerCase().replace(/[^\w ]/g, '');
    return stripped.split(' ').slice(0,3).join('-');
  },

  onCodeChange: function(newCode) {
    this.setState({
      nodeFunction: newCode
    });
  },

  onRegexTriggerChange: function(newRegexTrigger) {
    this.setState({
      regexTrigger: newRegexTrigger
    });
  },

  addParam: function() {
    var newParamIndex = this.state.params.length + 1;
    while (this.state.params.some(function(param) {
      return param.name == 'userInput' + newParamIndex;
    })) {
      newParamIndex++;
    }
    var newParamName = 'userInput' + newParamIndex;
    var newParam = { name: newParamName, question: '' };
    this.setState({
      params: this.state.params.concat([newParam])
    }, this.focusOnLastParam);
  },

  replaceParamAtIndexWithParam: function(index, newParam) {
    var newParams = BehaviorEditorUtils.arrayWithNewElementAtIndex(this.state.params, newParam, index);
    this.setState({
      params: newParams
    });
  },

  deleteParamAtIndex: function(index) {
    var newParams = this.state.params.slice();
    newParams.splice(index, 1);
    this.setState({
      params: newParams
    });
  },

  onTriggerChange: function(index, newTrigger) {
    var newTriggers = BehaviorEditorUtils.arrayWithNewElementAtIndex(this.state.triggers, newTrigger, index);
    this.setState({
      triggers: newTriggers
    });
  },

  render: function() {
    return (
      <form>
        <div className="form-field-group">
          <p><strong>In one phrase, describe what this behavior does.</strong></p>
          <p>You may also choose a short code name for reference.</p>

          <BehaviorEditorDescription description={this.state.description}
            codeName={this.getCodeName()}
            onChange={this.onDescriptionChange}
          />
        </div>

        <div className="form-field-group">
          <p><strong>Implement the behavior by writing a node.js function.</strong></p>

          <p>If you need to collect information from the user, add one or more parameters
          to your function. For each one, include a question for @ellipsis to ask the user.</p>

          <p>
            <span>Along with any parameters you’ve specified, your function will receive </span>
            <code>onSuccess</code> and <code>onError</code><span> callbacks, as well a </span>
            <code>context</code> object.
          </p>

          <div>
            <div>
              <code className="type-weak type-s">{"function ("}</code>
            </div>
            <div className="plxl">
              {this.state.params.map(function(param, index) {
                return (
                  <BehaviorEditorUserInputDefinition
                    key={'BehaviorEditorUserInputDefinition' + index}
                    ref={'param' + index}
                    name={param.name}
                    question={param.question}
                    onChange={this.replaceParamAtIndexWithParam.bind(this, index)}
                    onDelete={this.deleteParamAtIndex.bind(this, index)}
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
              <Codemirror value={this.state.nodeFunction}
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
          <p><strong>Specify one or more words or phrases that should trigger this behavior in chat.</strong></p>
          <div className="form-grouped-inputs mbl">
          {this.state.triggers.map(function(trigger, index) {
            return (
              <BehaviorEditorInput
                key={'BehaviorEditorTrigger' + index}
                ref={'trigger' + index}
                value={trigger}
                onChange={this.onTriggerChange.bind(this, index)}
              />
            );
          }, this)}
          </div>
          <button type="button" onClick={this.addMoreTriggers}>Add more triggers</button>
        </div>

        <div className="form-field-group">
          <p><strong>If desired, you can also specify a trigger that includes required values.</strong></p>
          <p>Write a regular expression pattern to match the trigger and capture the desired input.</p>
          <div className="">
            <Codemirror value={this.state.regexTrigger}
              onChange={this.onRegexTriggerChange}
              options={{ mode: "javascript", viewportMargin: Infinity }}
            />
          </div>
        </div>
        <button type="button" className="primary">Save and return</button>

      </form>
    );
  }
});

var BehaviorEditorDescription = React.createClass({
  handleChange: function(event) {
    this.props.onChange(event.target.value);
  },

  render: function() {
    return (
      <div className="form-grouped-inputs">
        <input type="text"
          className="form-input"
          placeholder="Bang two coconuts together"
          autoFocus
          value={this.props.description}
          onChange={this.handleChange}
        />
        <input type="text"
          className="form-input type-monospace"
          placeholder="bang-two-coconuts"
          readOnly
          value={this.props.codeName}
        />
      </div>
    );
  }
});

var BehaviorEditorUserInputDefinition = React.createClass({
  onChange: function(event) {
    this.props.onChange({ name: this.refs.name.value, question: this.refs.question.value });
  },

  onDeleteClick: function() {
    this.props.onDelete();
  },

  focus: function() {
    this.refs.question.focus();
  },

  render: function() {
    return (
      <div className={"columns " + (this.props.hasMargin ? "mts" : "")}>
        <div className="column column-one-quarter">
          <div className="columns columns-elastic">
            <div className="column column-expand prs">
              <input type="text"
                ref="name"
                className="form-input form-input-borderless type-monospace type-s"
                placeholder="userInput"
                value={this.props.name}
                onChange={this.onChange}
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
                <input type="text"
                  id={"question" + this.props.id}
                  ref="question"
                  className="form-input"
                  placeholder="Write a question to ask the user for this parameter"
                  autoFocus={this.props.shouldGrabFocus}
                  value={this.props.question}
                  onChange={this.onChange}
                />
              </div>
            </div>
            <div className="column column-shrink">
              <button className="subtle shrink" type="button" onClick={this.onDeleteClick}>
                <img src="/assets/images/delete.svg"
                  alt={"Delete"}
                  title={"Delete the “" + this.props.name + "” parameter"}
                />
              </button>
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

  isEmpty: function() {
    return !this.refs.input.value;
  },

  focus: function() {
    this.refs.input.focus();
  },

  render: function() {
    return (
      <input type="text" className="form-input"
        ref="input"
        value={this.props.value}
        onChange={this.onChange}
      />
    );
  }
});
