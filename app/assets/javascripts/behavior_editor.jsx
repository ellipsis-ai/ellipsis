var BehaviorEditor = React.createClass({
  render: function() {
    return (
      <form>
        <div className="form-field-group">
          <p><strong>In one phrase, describe what this behavior does.</strong></p>
          <p>You may also choose a short code name for reference.</p>
          <BehaviorEditorDescription description={this.props.description} />
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
    var stripped = this.state.value.toLowerCase().replace(/[^a-z\s]/g, '');
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
          readonly
          value={this.getCodeName()}
        />
      </div>
    );
  }
});
