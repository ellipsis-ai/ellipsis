// @flow
define(function(require) {
  const React = require('react');
  const DynamicLabelButton = require('../form/dynamic_label_button');
  const HelpButton = require('../help/help_button');
  const moment = require('moment');
  const autobind = require('../lib/autobind');

  type Detail = {
    kind: string,
    type: string,
    lastSaveTimestamp: string,
    lastDeployTimestamp?: string,
    onDevModeChannelsClick: () => void,
    onClick: (callback: () => void) => void
  }

  type Props = {
    details: Array<Detail>
  }

  class NotificationForDeploymentWarning extends React.PureComponent<Props> {
    props: Props;

    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        isDeploying: false
      };
    }

    detail(): ?Detail {
      return this.props.details.find((detail) => detail.type === "saved_version_not_deployed");
    }

    deploy(): void {
      this.setState({
        isDeploying: true
      }, () => {
        const detail = this.detail();
        if (detail) {
          detail.onClick(() => {
            this.setState({ isDeploying: false });
          });
        }
      });
    }

    isDeploying(): boolean {
      return this.state && this.state.isDeploying;
    }

    durationSinceDeployment(detail: ?Detail): ?string {
      return detail && detail.lastDeployTimestamp ?
        moment(detail.lastDeployTimestamp).from(new Date(detail.lastSaveTimestamp), true) :
        null;
    }

    versionStatusText(detail: ?Detail): string {
      const duration = this.durationSinceDeployment(detail);
      if (duration) {
        return `This version is ${duration} newer than the deployed skill.`;
      } else {
        return `This skill has not yet been deployed to your team.`;
      }
    }

    getConfirmButtonLabels(hasDeployed: boolean): Array<{ text: string, displayWhen: boolean }> {
      return [
        {
          text: "Deploying…",
          displayWhen: this.isDeploying()
        },
        {
          text: "Deploy this version",
          displayWhen: hasDeployed && !this.isDeploying()
        },
        {
          text: "Deploy skill",
          displayWhen: !hasDeployed && !this.isDeploying()
        }
      ];
    }

    onHelpClick(): void {
      const detail = this.detail();
      if (detail) {
        detail.onDevModeChannelsClick();
      }
    }

    render(): React.Node {
      const detail = this.detail();
      const hasDeployed = Boolean(detail && detail.lastDeployTimestamp);
      return (
        <div className="columns columns-elastic">
          <div className="column column-expand">
            <span>{this.versionStatusText(detail)} </span>
            <span>To test {hasDeployed ? "this version" : "the skill"} before deploying, </span>
            <span className="type-bold">use any channel in dev mode. </span>
            <HelpButton onClick={this.onHelpClick} className="mhxs" />
          </div>
          <div className="column column-shrink">
            <DynamicLabelButton
              className="button-s button-shrink mrs"
              onClick={this.deploy}
              labels={this.getConfirmButtonLabels(hasDeployed)}
              disabledWhen={this.isDeploying()}
            />
          </div>
        </div>
      );
    }
  }

  return NotificationForDeploymentWarning;
});
