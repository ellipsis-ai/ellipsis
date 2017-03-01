define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      onClick: React.PropTypes.func.isRequired,
      label: React.PropTypes.string
    },

    render: function() {
      return (
        <button type="button"
          onClick={this.props.onClick}
          className="button button-s button-shrink">{this.props.label || "Add another"}</button>
      );
    }
  });

});

