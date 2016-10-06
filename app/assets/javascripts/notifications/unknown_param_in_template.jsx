define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'NotificationForUnknownParamInTemplate',
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired
      })).isRequired
    },

    render: function() {
      var numParams = this.props.details.length;
      if (numParams === 1) {
        let detail = this.props.details[0];
        return (
          <span>The response contains an unknown variable name: <code>{detail.name}</code></span>
        );
      } else {
        return (
          <span>
            <span>The response contains unknown variable names: </span>
            {this.props.details.map((detail, index) => (
              <span key={`unknownParamName${index}`}>
                <code className="mhxs type-bold">{detail.name}</code>
                <span className="type-weak">{index + 1 < numParams ? " · " : ""}</span>
              </span>
            ))}
          </span>
        );
      }
    }
  });
});
