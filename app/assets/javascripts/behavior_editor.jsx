define('behavior_editor', function(require) {

var React = require('react');
var ReactDOM = require('react-dom');
var Codemirror = require('react-codemirror');

var BehaviorEditor = React.createClass({
  getInitialState: function() {
    return {
      description: this.props.description,
      nodeFunction: this.props.nodeFunction,
      argNames: this.props.argNames
    };
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
      argNames: this.getArgumentNamesFromCode(newCode)
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
    return args.slice(0, args.length - 2); // last two arguments are reserved for onSuccess/onError
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
          <p><strong>Specify what @ellipsis should do by writing a node.js function.</strong></p>
          <p>If you want your behavior to collect input from the user before operation, specify
          each required value as an argument to the function.</p>

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
          <p><strong>For each required value, write what question @ellipsis should ask the user.</strong></p>

          {this.state.argNames.map(function(argName, index) {
            return (
              <div className="form-field-with-prefix mbs" key={'argName' + index}>
                <label className="form-input-prefix"><code>{argName}</code></label>
                <input type="text" className="form-input" value="" placeholder="Where would you like to go?" />
              </div>
            )
          })}
        </div>

        <div className="form-field-group">
          <p><strong>Specify one or more words or phrases that should trigger this behavior in chat.</strong></p>
          <div className="form-grouped-inputs mbl">
            <input type="text" className="form-input" placeholder="ride" />
            <input type="text" className="form-input" placeholder="trot" />
            <input type="text" className="form-input" placeholder="gallop" />
          </div>
          <button type="button">Add more triggers</button>
        </div>

        <div className="form-field-group">
          <p><strong>If desired, you can also specify a trigger that includes required values.</strong></p>
          <p>Write a regular expression pattern to match the trigger and capture the desired input.</p>
          <div className="form-field">
            <Codemirror value="/ride\sto\s+(.+)/"
              onChange={function(){}}
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
          autofocus
          value={this.props.description}
          onChange={this.handleChange}
        />
        <input type="text"
          className="form-input"
          placeholder="bang-two-coconuts"
          readOnly
          value={this.props.codeName}
        />
      </div>
    );
  }
});

return {
  load: function(behaviorEditorOptions) {
    React.createElement(BehaviorEditor, behaviorEditorOptions);
    ReactDOM.render(myBehaviorEditor, document.getElementById('editorContainer'));
  }
};

});

