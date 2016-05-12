var BehaviorEditorUtils = {
  arrayWithNewElementAtIndex: function(array, newElement, index) {
    // Create a copy of the old array with the indexed element replaced
    return array.slice(0, index).concat([newElement], array.slice(index + 1, array.length));
  }
};

var BehaviorEditor = React.createClass({
  propTypes: {
    description: React.PropTypes.string,
    nodeFunction: React.PropTypes.string,
    args: React.PropTypes.arrayOf(React.PropTypes.shape({
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
      args: this.props.args,
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
      return key.match(/trigger\d+/) && !this.refs[key].value;
    }, this);
    if (blankTrigger) {
      this.refs[blankTrigger].focus();
    }
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
      nodeFunction: newCode,
      args: this.getArgumentNamesFromCode(newCode)
    });
  },

  onRegexTriggerChange: function(newRegexTrigger) {
    this.setState({
      regexTrigger: newRegexTrigger
    });
  },

  updateCodeFromArgs: function() {
    var newFunction = this.state.nodeFunction.replace(/^\s*function\(.+?\)/, this.getFunctionPrefix());
    this.setState({
      nodeFunction: newFunction
    });
  },

  onAddQuestionClick: function() {
    var newArg = { name: 'userInput' + (this.state.args.length + 1), question: '' };
    this.setState({
      args: this.state.args.concat([newArg]),
      questionFocusIndex: this.state.args.length
    }, this.updateCodeFromArgs);
  },

  onArgChange: function(index, newArg) {
    var newArgs = BehaviorEditorUtils.arrayWithNewElementAtIndex(this.state.args, newArg, index);
    this.setState({
      args: newArgs
    }, this.updateCodeFromArgs);
  },

  onTriggerChange: function(index, event) {
    var newTrigger = event.target.value;
    var newTriggers = BehaviorEditorUtils.arrayWithNewElementAtIndex(this.state.triggers, newTrigger, index);
    this.setState({
      triggers: newTriggers
    });
  },

  getArgumentNamesFromCode: function(code) {
    var matches = code.match(/^\s*function\((.+)\)/);
    if (!matches || matches.length < 2) {
      return [];
    }
    var args = matches[1].split(',').map(function(arg) {
      return arg.replace(/(^\s*)|(\s*$)/g, ''); // trim spaces
    });

    // last two arguments are reserved for onSuccess/onError
    return args.slice(0, args.length - 2).map(function(arg, index) {
      return {
        name: arg,
        question: this.state.args[index] ? this.state.args[index].question : ''
      };
    }, this);
  },

  getFunctionPrefix: function() {
    return 'function(' +
      this.state.args.map(function(arg) { return arg.name; }).join(', ') +
      ', onSuccess, onError)';
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
          <p><strong>What text should @ellipsis collect from the user?</strong></p>

          <p>Write one or more questions to ask the user for input. Each one has a
          programming-friendly label that can be modified for clarity if desired.</p>

          {this.state.args.map(function(arg, index) {
            return (
              <BehaviorEditorUserInputDefinition key={index} name={arg.name} question={arg.question}
                onChange={this.onArgChange.bind(this, index)} shouldGrabFocus={this.state.questionFocusIndex == index} />
            );
          }, this)}

          <button type="button" onClick={this.onAddQuestionClick}>Add another question</button>
        </div>

        <div className="form-field-group">
          <p><strong>Specify what @ellipsis should do by writing a node.js function.</strong></p>
          <p>Each fragment of text collected from the user will be passed to the function, along
          with <code>onSuccess</code> and <code>onError</code> callbacks that you should call with the
          appropriate response.</p>

          <div className="form-field">
            <Codemirror value={this.state.nodeFunction}
              onChange={this.onCodeChange}
              options={{
                lineNumbers: true,
                mode: "javascript",
                viewportMargin: Infinity
              }}
            />
          </div>
        </div>

        <div className="form-field-group">
          <p><strong>Specify one or more words or phrases that should trigger this behavior in chat.</strong></p>
          <div className="form-grouped-inputs mbl">
          {this.state.triggers.map(function(trigger, index) {
            return (
              <input type="text" className="form-input"
                key={index}
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
          <div className="form-field">
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
  onQuestionChange: function(event) {
    this.props.onChange({ name: this.props.name, question: event.target.value });
  },

  onNameChange: function(event) {
    this.props.onChange({ name: event.target.value, question: this.props.question });
  },

  render: function() {
    return (
      <div className="form-grouped-inputs mbs">
        <input type="text"
          className="form-input"
          placeholder="Where would you like to go?"
          autoFocus={this.props.shouldGrabFocus}
          value={this.props.question}
          onChange={this.onQuestionChange}
        />
        <input type="text"
          className="form-input type-monospace"
          placeholder="userInput"
          value={this.props.name}
          onChange={this.onNameChange}
        />
      </div>
    );
  }
});
