define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'NotificationForEnvVarMissing',
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        environmentVariableName: React.PropTypes.string.isRequired,
        onClick: React.PropTypes.func.isRequired
      })).isRequired
    },

    getButtonForEnvVar: function(envVarDetail) {
      return (
        <button
          type="button"
          className="button-raw button-s type-monospace type-bold mlxs"
          onClick={envVarDetail.onClick}
        >{envVarDetail.environmentVariableName}</button>
      );
    },

    render: function() {
      var numVarsMissing = this.props.details.length;
      if (numVarsMissing === 1) {
        var firstDetail = this.props.details[0];
        return (
          <span>
            <span>This behavior requires an environment variable named </span>
            {this.getButtonForEnvVar(firstDetail)}
            <span className="mlxs"> to work properly.</span>
          </span>
        );
      } else {
        return (
          <span>
            <span>This behavior requires the following environment variables to work properly: </span>
            {this.props.details.map(function(detail, index) {
              return (
                <span key={"notificationDetail" + index}>
                  {this.getButtonForEnvVar(detail)}
                  <span>{index + 1 < numVarsMissing ? ", " : ""}</span>
                </span>
              );
            }, this)}
          </span>
        );
      }
    }
  });
});
