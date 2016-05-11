var BehaviorEditor = React.createClass({
  render: function() {
    return (
      <form>
        <div className="form-field-group">
          <p><strong>In one phrase, describe what this behavior does.</strong></p>
          <p>You may also choose a short code name for reference.</p>
          <BehaviorEditorDescription description={this.props.description} />
        </div>

        <div className="form-field-group">
          <p><strong>Specify what @ellipsis should do by writing a node.js function.</strong></p>
          <p>If you want your behavior to collect input from the user before operation, specify
          each required value as an argument to the function.</p>
          <div className="form-field">
            <BehaviorEditorCodeInput code={this.props.nodeFunction} lineNumbers />
          </div>
        </div>
      </form>
    );
  }
});

var BehaviorEditorDescription = React.createClass({
  getInitialState: function() {
    return {
      value: this.props.description
    };
  },

  handleChange: function(event) {
    this.setState({
      value: event.target.value
    });
  },

  getCodeName: function() {
    var stripped = this.state.value.toLowerCase().replace(/[^\w ]/g, '');
    return stripped.split(' ').slice(0,3).join('-');
  },

  render: function() {
    return (
      <div className="form-grouped-inputs">
        <input type="text"
          className="form-input"
          placeholder="Bang two coconuts together"
          autofocus
          value={this.state.value}
          onChange={this.handleChange}
        />
        <input type="text"
          className="form-input"
          placeholder="bang-two-coconuts"
          readOnly
          value={this.getCodeName()}
        />
      </div>
    );
  }
});

var BehaviorEditorCodeInput = React.createClass({
  getInitialState: function() {
    return {
      code: this.props.code
    };
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

  updateCode: function(newCode) {
    var argumentNames = this.getArgumentNamesFromCode(newCode);
    this.setState({
      code: newCode,
      argumentNames: argumentNames
    });
    console.log(argumentNames);
  },

  render: function() {
    var options = {
      lineNumbers: this.props.lineNumbers || false,
      mode: "javascript",
      viewportMargin: Infinity
    };
    return (
      <Codemirror
        value={this.state.code}
        onChange={this.updateCode}
        options={options}
      />
    );
  }
});
