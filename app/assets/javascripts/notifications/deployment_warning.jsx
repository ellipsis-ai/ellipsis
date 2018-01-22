// @flow

define(function(require) {
  const React = require('react');
  const moment = require('moment');
  const autobind = require('../lib/autobind');

    class NotificationForDeploymentWarning extends React.Component {

    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        isDeploying: false
      };
    }

    detail() {
      return this.props.details.find((detail) => detail.type === "saved_version_not_deployed");
    }

    deploy() {
      this.setState({
        isDeploying: true
      }, () => {
        this.detail().onClick(() => {
          this.setState({ isDeploying: false });
        });
      });
    }

    isDeploying() {
      return this.state && this.state.isDeploying;
    }

    timestampText() {
      return moment(this.detail().lastDeployTimestamp).from(new Date(this.detail().lastSaveTimestamp), true);
    }

    versionStatusText() {
      if (this.detail().lastDeployTimestamp) {
        return `This version is ${this.timestampText()} newer than the last deployed version.`;
      } else {
        return `This skill hasn't yet been deployed, so it isn't generally available to users.`;
      }
    }

    render() {
      return (
        <span>
          <span>{this.versionStatusText()} Use </span>
          <button className="button-raw" type="button" onClick={this.detail().onDevModeChannelsClick}>dev mode channels</button>
          <span> to test until it has been deployed , or</span>
          <button className="button-s button-shrink mls" type="button" onClick={this.deploy}>
            {this.isDeploying() ? "Deploying…" : "Deploy now"}
          </button>
        </span>
      );
    }
  }

  NotificationForDeploymentWarning.propTypes = {
    details: React.PropTypes.arrayOf(React.PropTypes.shape({
      kind: React.PropTypes.string.isRequired,
      type: React.PropTypes.string.isRequired,
      lastSaveTimestamp: React.PropTypes.string.isRequired,
      lastDeployTimestamp: React.PropTypes.string,
      onDevModeChannelsClick: React.PropTypes.func.isRequired,
      onClick: React.PropTypes.func.isRequired
    })).isRequired
  };

  return NotificationForDeploymentWarning;
});
