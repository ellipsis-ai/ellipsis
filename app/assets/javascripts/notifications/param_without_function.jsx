define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired
      })),
      onClick: React.PropTypes.func.isRequired
    },

    onClick: function(detail) {
      this.props.onClick(detail);
    },

    render: function() {
      var paramNames = this.props.details.map((ea) => ea.name);
      return (
        <span>
          <span>If your behavior is going to run code, the function can receive any </span>
          <span>trigger fill-in-the-blanks as parameters. </span>
          <button type="button"
            className="button-raw"
            onClick={this.onClick.bind(this, {
              kind: "param_without_function",
              paramNames: paramNames
            })}
          >Add code with parameters</button>
        </span>
      );
    }
  });
});
