define(function(require) {
  var React = require('react');

  class NotificationForServerDataWarning extends React.Component {

    getNetworkErrorMessage(networkError) {
      const errorMessage = networkError.error && networkError.error.message ? networkError.error.message : "";
      if (/^401/.test(errorMessage)) {
        return "You have been signed out. Please reload the page.";
      } else if (/^404/.test(errorMessage)) {
        return "This skill may have been deleted on the server by someone else.";
      } else if (/^5\d\d/.test(errorMessage)) {
        return "The Ellipsis server responded with an error. You may need to reload the page.";
      } else {
        return "The Ellipsis server cannot be reached. Your connection may be down.";
      }
    }

    getNewerVersionMessage(newerVersion) {
      if (newerVersion.isSameUser) {
        return "You have saved a newer version of this skill in another window.";
      } else {
        return "Someone else has saved a newer version of this skill.";
      }
    }

    render() {
      const newerVersion = this.props.details.find((detail) => detail.type === "newer_version");
      const networkError = this.props.details.find((detail) => detail.type === "network_error");
      let message = "";
      if (networkError) {
        message += this.getNetworkErrorMessage(networkError) + " ";
      }
      if (newerVersion) {
        message += this.getNewerVersionMessage(newerVersion);
      }

      return (
        <span>
          <span className="type-label">Warning: </span>
          <span className="mrs">{message}</span>
          {newerVersion && newerVersion.onClick && !networkError ? (
            <button className="button-s button-inverted" type="button" onClick={newerVersion.onClick}>Reload the editor</button>
          ) : null}
        </span>
      );
    }
  }

  NotificationForServerDataWarning.propTypes = {
    details: React.PropTypes.arrayOf(React.PropTypes.shape({
      kind: React.PropTypes.string.isRequired,
      type: React.PropTypes.string.isRequired,
      onClick: React.PropTypes.func,
      error: React.PropTypes.instanceOf(Error),
      isSameUser: React.PropTypes.bool
    })).isRequired
  };

  return NotificationForServerDataWarning;
});
