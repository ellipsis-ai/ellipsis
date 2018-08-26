import * as React from 'react';
import {OAuthApplicationRef, RequiredOAuthApplication} from '../models/oauth';
import OAuthConfigWithoutApplicationNotificationData from "../models/notifications/oauth_config_without_application_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<OAuthConfigWithoutApplicationNotificationData>
}

class NotificationForMissingOAuthApplication extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  recommendedScopeAnnotation(detail: OAuthConfigWithoutApplicationNotificationData) {
    var recommendedScope = detail.requiredApiConfig.recommendedScope;
    if (recommendedScope) {
      return (
        <span>
            <span>(recommended scope: </span>
            <b>{recommendedScope}</b>
            <span>)</span>
          </span>
      );
    } else {
      return null;
    }
  }

  addOAuthApplicationPrompt(detail: OAuthConfigWithoutApplicationNotificationData) {
    var matchingApplications = detail.existingOAuthApplications.filter(ea => ea.apiId === detail.requiredApiConfig.apiId);
    if (matchingApplications.length === 1) {
      const app = matchingApplications[0];
      return (
        <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onUpdateOAuthApplication.bind(this, detail, app)}>

              Add {app.displayName} to this skill

            </button>
          </span>
      );
    } else if (matchingApplications.length === 0) {
      return (
        <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onNewOAuthApplication.bind(this, detail, detail.requiredApiConfig)}>

              Configure the {detail.name} API for this skill

            </button>
          </span>
      );
    } else {
      return (
        <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={detail.onConfigClick.bind(this, detail.requiredApiConfig)}>

              Choose a configuration for {detail.requiredApiConfig.codePath()}

            </button>
          </span>
      );
    }
  }

  onUpdateOAuthApplication(detail: OAuthConfigWithoutApplicationNotificationData, app: OAuthApplicationRef) {
    detail.onUpdateOAuthApplication(detail.requiredApiConfig.clone({
      config: app
    }));
  }

  onNewOAuthApplication(detail: OAuthConfigWithoutApplicationNotificationData, requiredOAuthApiConfig: RequiredOAuthApplication) {
    detail.onNewOAuthApplication(requiredOAuthApiConfig);
  }

  render() {
    var numRequiredApiConfigs = this.props.details.length;
    if (numRequiredApiConfigs === 1) {
      var detail = this.props.details[0];
      return (
        <span>
            <span>This skill needs to be configured to use the <b>{detail.name}</b> API {this.recommendedScopeAnnotation(detail)}.</span>
          {this.addOAuthApplicationPrompt(detail)}
          </span>
      );
    } else {
      return (
        <span>
            <span>This skill needs to be configured to use the following APIs: </span>
          {this.props.details.map((ea, index) => {
            return (
              <span key={"oAuthNotificationDetail" + index}>
                  <span>{ea.name} {this.recommendedScopeAnnotation(ea)}</span>
                {this.addOAuthApplicationPrompt(ea)}
                <span>{index + 1 < numRequiredApiConfigs ? ", " : ""}</span>
                </span>
            );
          })}
          </span>
      );
    }
  }
}

export default NotificationForMissingOAuthApplication;
