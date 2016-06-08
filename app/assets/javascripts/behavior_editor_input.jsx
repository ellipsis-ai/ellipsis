define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'BehaviorEditorInput',
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
      <div className="columns columns-elastic">
        <div className="column column-expand position-relative">
          <input type="text"
            className={"form-input " + (this.props.className || "")}
            ref="input"
            id={this.props.id}
            value={this.props.value}
            placeholder={this.props.placeholder}
            autoFocus={this.props.autoFocus}
            onChange={this.onChange}
            onKeyPress={this.handleEnterKey}
          />
        </div>
        <div className="column column-shrink align-m">
          <div className="display-ellipsis type-label">
            <span className="mrm">
              <input type="checkbox" /> Regexp
            </span>
            <span className="">
              <input type="checkbox" /> Case
            </span>
          </div>
        </div>
      </div>
    );
  }
});

});
