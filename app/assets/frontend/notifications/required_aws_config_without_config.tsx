import * as React from 'react';
import {AWSConfigRef, RequiredAWSConfig} from '../models/aws';
import RequiredAwsConfigNotificationData from "../models/notifications/required_aws_config_notification_data";

interface Props {
  details: Array<RequiredAwsConfigNotificationData>
}

class NotificationForMissingAWSConfig extends React.PureComponent<Props> {
    addAWSConfigPrompt(detail: RequiredAwsConfigNotificationData) {
      var matchingConfigs = detail.existingAWSConfigs;
      if (matchingConfigs.length === 1) {
        const config = matchingConfigs[0];
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onUpdateAWSConfig.bind(this, detail, config)}>

              Add {config.displayName} to this skill

            </button>
          </span>
        );
      } else if (matchingConfigs.length === 0) {
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onNewAWSConfig.bind(this, detail, detail.requiredAWSConfig)}>

              Configure the AWS API for this skill

            </button>
          </span>
        );
      } else {
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={detail.onConfigClick.bind(this, detail.requiredAWSConfig)}>

              Choose a configuration for {detail.requiredAWSConfig.codePath()}

            </button>
          </span>
        );
      }
    }

    onUpdateAWSConfig(detail: RequiredAwsConfigNotificationData, cfg: AWSConfigRef): void {
      detail.onUpdateAWSConfig(detail.requiredAWSConfig.clone({
        config: cfg
      }));
    }

    onNewAWSConfig(detail: RequiredAwsConfigNotificationData, requiredAWSConfig: RequiredAWSConfig): void {
      detail.onNewAWSConfig(requiredAWSConfig);
    }

    render() {
      var detail = this.props.details[0];
      return (
        <span>
          <span>This skill needs to be configured to use the <b>AWS</b> API.</span>
          {this.addAWSConfigPrompt(detail)}
        </span>
      );
    }
}

export default NotificationForMissingAWSConfig;
