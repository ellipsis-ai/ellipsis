define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      data: React.PropTypes.object.isRequired,
      index: React.PropTypes.number.isRequired
    },

    getNotificationForKind: function(kind) {
      if (kind === "env_var_not_defined") {
        return (
          <span>
            <span>This behavior requires an environment variable named </span>
            <code className="type-bold">{this.props.data.environmentVariableName}</code>
            <span> to work properly.</span>
          </span>
        );
      }
    },

    render: function() {
      return (
        <div className="box-warning type-xs"
          style={{
            marginTop: (this.props.index > 0 ? -1 : 0),
            zIndex: 1
          }}
        >
          <div className="container">
            {this.getNotificationForKind(this.props.data.kind)}
          </div>
        </div>
      );
    }
  });
});
