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
      var numParams = this.props.details.length;
      if (numParams === 1) {
        let detail = this.props.details[0];
        return (
          <span>
            <span>You’ve added a parameter in your triggers. Now add it to your </span>
            <span>function to use it in code: </span>
            <button type="button"
              className="button-raw type-monospace"
              onClick={detail.onClick}
            >{detail.name}</button>
          </span>
        );
      } else {
        return (
          <span>
            <span>You’ve added some parameters in your triggers. Now add them to your </span>
            <span>function to use them in code: </span>
            {this.props.details.map((detail, index) => (
              <span key={`unusedParamName${index}`}>
                  <button type="button"
                    className="button-raw type-monospace"
                    onClick={detail.onClick}
                  >{detail.name}</button>
                  <span>{index + 1 < numParams ? ", " : ""}</span>
                </span>
            ))}
          </span>
        );
      }
    }
  });
});
