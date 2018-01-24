// @flow

define(function(require) {
  const DynamicLabelButton = require('../form/dynamic_label_button');
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
      return this.props.details.find((detail) => detail.type === "saved_version_not_deployed") || {};
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

    isDeploying(): boolean {
      return this.state && this.state.isDeploying;
    }

    timestampText(): string {
      return moment(this.detail().lastDeployTimestamp).from(new Date(this.detail().lastSaveTimestamp), true);
    }

    versionStatusText(): string {
      if (this.detail().lastDeployTimestamp) {
        return `This version is ${this.timestampText()} newer than the last deployed version.`;
      } else {
        return `This skill hasn't yet been deployed, so it isn't generally available to users.`;
      }
    }

    getConfirmButtonLabels() {
      return [
        {
          text: "Deploying…",
          displayWhen: this.isDeploying()
        },
        {
          text: "Deploy now",
          displayWhen: !this.isDeploying()
        }
      ];
    }

    render() {
      return (
        <span>
          <span>{this.versionStatusText()} Use </span>
          <button className="button-raw" type="button" onClick={this.detail().onDevModeChannelsClick}>dev mode channels</button>
          <span> to test until it has been deployed , or</span>
          <DynamicLabelButton
            className="button-s button-shrink mls"
            onClick={this.deploy}
            labels={this.getConfirmButtonLabels()}>
          </DynamicLabelButton>
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