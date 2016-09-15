define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        onClick: React.PropTypes.func.isRequired
      })).isRequired
    },

    render: function() {
      return (
        <span>
          <span>If your behavior is going to run code, the function can receive any </span>
          <span>trigger fill-in-the-blanks as parameters. </span>
          <button type="button"
            className="button-raw"
            onClick={this.props.details[0].onClick}
          >Add code with parameters</button>
        </span>
      );
    }
  });
});
