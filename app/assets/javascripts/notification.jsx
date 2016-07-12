define(function(require) {
  var React = require('react'),
    SVGWarning = require('./svg/warning');

  return React.createClass({
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      kind: React.PropTypes.string.isRequired,
      index: React.PropTypes.number.isRequired
    },

    getNotificationForKind: function(kind) {
      if (kind === "env_var_not_defined") {
        return this.getNotificationForEnvVarMissing();
      }
    },

    getNotificationForEnvVarMissing: function() {
      var numVarsMissing = this.props.details.length;
      if (numVarsMissing === 1) {
        return (
          <span>
            <span>This behavior requires an environment variable named </span>
            <code className="type-bold">{this.props.details[0].environmentVariableName}</code>
            <span> to work properly.</span>
          </span>
        );
      } else {
        return (
          <span>
            <span>This behavior requires the following environment variables to work properly: </span>
            {this.props.details.map(function(detail, index) {
              return (
                <span key={"notificationDetail" + index}>
                  <button
                    type="button"
                    className="button-raw button-s type-monospace type-bold mlxs"
                    onClick={this.onClick.bind(this, detail)}
                  >{detail.environmentVariableName}</button>
                  <span>{index + 1 < numVarsMissing ? ", " : ""}</span>
                </span>
              );
            }, this)}
          </span>
        );
      }
    },

    onClick: function(notificationDetail) {
      if (this.props.onClick) {
        this.props.onClick(notificationDetail);
      }
    },

    render: function() {
      return (
        <div className="box-warning type-s phn"
          style={{
            marginTop: -1,
            zIndex: 1
          }}
        >
          <div className="container">
            <span className="display-inline-block mrs align-b type-yellow" style={{ height: 24 }}>
              <SVGWarning />
            </span>
            {this.getNotificationForKind(this.props.kind)}
          </div>
        </div>
      );
    }
  });
});
