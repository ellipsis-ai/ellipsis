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
  },

  getInitialState: function() {
    return {
      behaviorId: this.props.behaviorId,
      description: this.props.description,
      nodeFunction: this.props.nodeFunction,
      params: this.props.params,
      triggers: this.props.triggers
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

  deleteTriggerAtIndex: function(index) {
    this.setState({
      triggers: this.utils.arrayRemoveElementAtIndex(this.state.triggers, index)
    });
  },

  focusOnLastParam: function() {
    this.refs['param' + (this.state.params.length - 1)].focus();
  },

  onDescriptionChange: function(newDescription) {
    this.setState({ description: newDescription });
  },

  onCodeChange: function(newCode) {
    this.setState({
      nodeFunction: newCode
    });
  },

  addParam: function() {
    var newParamIndex = this.state.params.length + 1;
    while (this.state.params.some(function(param) {
      return param.name == 'userInput' + newParamIndex;
    })) {
      newParamIndex++;
    }
    this.setState({
      params: this.state.params.concat([{ name: 'userInput' + newParamIndex, question: '' }])
    }, this.focusOnLastParam);
  },

  replaceParamAtIndexWithParam: function(index, newParam) {
    this.setState({
      params: this.utils.arrayWithNewElementAtIndex(this.state.params, newParam, index)
    });
  },

  deleteParamAtIndex: function(index) {
    this.setState({
      params: this.utils.arrayRemoveElementAtIndex(this.state.params, index)
    });
  },

  onTriggerChange: function(index, newTrigger) {
    this.setState({
      triggers: this.utils.arrayWithNewElementAtIndex(this.state.triggers, newTrigger, index)
    });
  },

  render: function() {
    return (
      <form action="/save_behavior" method="POST">
        <BehaviorEditorHiddenJsonInput
          value={JSON.stringify(this.state)}
        />
        <div className="form-field-group">
          <h3 className="mtxxxxl mbn type-weak">Edit behavior</h3>
          <BehaviorEditorDescription description={this.state.description}
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
              <div className="columns columns-elastic" key={'BehaviorEditorTriggerContainer' + index}>
                <div className="column column-expand prxs">
                  <BehaviorEditorInput
                    key={'BehaviorEditorTrigger' + index}
                    ref={'trigger' + index}
                    value={trigger}
                    onChange={this.onTriggerChange.bind(this, index)}
                  />
                </div>
                <div className="column column-shrink">
                  <BehaviorEditorDeleteButton
                    key={'BehaviorEditorTriggerDelete' + index}
                    onClick={this.deleteTriggerAtIndex.bind(this, index)}
                  />
                </div>
              </div>
            );
          }, this)}
          </div>
          <button type="button" onClick={this.addMoreTriggers}>Add more triggers</button>
        </div>

        <button type="submit" className="primary">Save and return</button>

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
      <input type="text"
        className="form-input form-input-borderless form-input-h2"
        placeholder="Describe the behavior in one phrase"
        value={this.props.description}
        onChange={this.handleChange}
      />
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
    this.refs.name.focus();
    this.refs.name.select();
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
      <button className="subtle shrink" type="button" onMouseUp={this.onClick} ref="button">
        <img src="/assets/images/delete.svg"
          alt="Delete"
          title={this.props.title || "Delete"}
        />
      </button>
    );
  }
});
