define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'ExpandButton',
    propTypes: {
      onToggle: React.PropTypes.func.isRequired,
      expandedWhen: React.PropTypes.bool.isRequired,
      children: React.PropTypes.node.isRequired,
      className: React.PropTypes.string
    },

    toggle: function() {
      this.props.onToggle();
    },

    render: function() {
      return (
        <button type="button" className={`button-raw ${this.props.className || ""}`} onClick={this.toggle}>
          <span>
            <span className="display-inline-block mrs" style={{ width: '0.8em' }}>
              {this.props.expandedWhen ? "▾" : "▸"}
            </span>
            <span>{this.props.children}</span>
          </span>
        </button>
      );
    }
  });
});
