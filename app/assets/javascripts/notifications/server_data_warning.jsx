define(function(require) {
  var React = require('react');

  class NotificationForServerDataWarning extends React.Component {
    getErrorMessage(networkError) {
      return networkError.error && networkError.error.message ? networkError.error.message : "";
    }

    render() {
      const newerVersion = this.props.details.find((detail) => detail.type === "newer_version");
      const networkError = this.props.details.find((detail) => detail.type === "network_error");
      let message = "Warning: ";
      if (networkError) {
        const errorMessage = this.getErrorMessage(networkError);
        if (/^401/.test(errorMessage)) {
          message += "You have been signed out. Please reload the page.";
        } else if (/^404/.test(errorMessage)) {
          message += "This skill may have been deleted on the server by someone else.";
        } else if (/^5\d\d/.test(errorMessage)) {
          message += "The Ellipsis server responded with an error. You may need to reload the page.";
        } else {
          message += "The Ellipsis server cannot be reached. Your connection may be down.";
        }
      }
      if (newerVersion && networkError) {
        message += " Also, a newer version of this skill has been saved by someone else.";
      } else if (newerVersion) {
        message += "A newer version of this skill has been saved by someone else.";
      }
      return (
        <span>
          <span className="mrs">{message}</span>
          {newerVersion && newerVersion.onClick && !networkError ? (
            <button className="button-s" type="button" onClick={newerVersion.onClick}>Reload the editor</button>
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
      error: React.PropTypes.instanceOf(Error)
    })).isRequired
  };

  return NotificationForServerDataWarning;
});
