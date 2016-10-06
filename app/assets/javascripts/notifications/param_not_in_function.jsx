define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'NotificationForParamNotInFunction',
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
            <span>You’ve specified a new input in your triggers. Add a definition for it to </span>
            <span>use it in your code or in the response: </span>
            <button type="button"
              className="button-raw type-monospace"
              onClick={detail.onClick}
            >{detail.name}</button>
          </span>
        );
      } else {
        return (
          <span>
            <span>You’ve added some new inputs in your triggers. Add definitions for them to </span>
            <span>use them in code or in the response: </span>
            {this.props.details.map((detail, index) => (
              <span key={`unusedParamName${index}`}>
                  <button type="button"
                    className="button-raw type-monospace mhxs"
                    onClick={detail.onClick}
                  >{detail.name}</button>
                  <span className="type-weak">{index + 1 < numParams ? " · " : ""}</span>
                </span>
            ))}
          </span>
        );
      }
    }
  });
});
