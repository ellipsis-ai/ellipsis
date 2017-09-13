define(function(require) {
  var React = require('react');

  class NotificationForServerDataWarning extends React.Component {
    render() {
      const newerVersion = this.props.details.find((detail) => detail.type === "newer_version");
      const networkError = this.props.details.find((detail) => detail.type === "network_error");
      let message = "Warning: ";
      if (newerVersion) {
        message += "a newer version of this skill has been saved by someone else. ";
      }
      if (newerVersion && networkError) {
        message += "However, the server cannot currently be reached.";
      } else if (!newerVersion && networkError) {
        message += "The server cannot currently be reached.";
      }
      return (
        <span>
          <span className="mrs">{message}</span>
          {newerVersion && !networkError ? (
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
      onClick: React.PropTypes.func.isRequired
    })).isRequired
  };

  return NotificationForServerDataWarning;
});
