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
    console.log(this.state.argNames);
  },

  getArgumentNamesFromCode: function(code) {
    var firstLine = code.split('\n')[0];
    var matches = firstLine.match(/^function\((.+)\)/);
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

