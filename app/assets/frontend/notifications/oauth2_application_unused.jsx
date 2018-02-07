import * as React from 'react';

const NotificationForUnusedOAuth2Application = React.createClass({
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        code: React.PropTypes.string.isRequired
      })).isRequired
    },

    render: function() {
      var numApps = this.props.details.length;
      if (numApps === 1) {
        var firstApp = this.props.details[0];
        return (
          <span>
            <span>Add <code className="box-code-example mhxs">{firstApp.code}</code> to your function to use the </span>
            <span>API token for <b>{firstApp.name}</b>. </span>
          </span>
        );
      } else {
        return (
          <span>
            <span>This skill has the following API tokens available: </span>
            {this.props.details.map((detail, index) => {
              return (
                <span key={"oAuthNotificationDetail" + index}>
                  <code className="box-code-example mhxs">{detail.code}</code>
                  <span>{index + 1 < numApps ? ", " : ""}</span>
                </span>
              );
            })}
          </span>
        );
      }
    }
});

export default NotificationForUnusedOAuth2Application;
