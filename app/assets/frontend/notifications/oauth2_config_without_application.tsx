import * as React from 'react';
import {OAuth2ApplicationRef, RequiredOAuth2Application} from '../models/oauth2';
import OAuth2ConfigWithoutApplicationNotificationData from "../models/notifications/oauth2_config_without_application_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<OAuth2ConfigWithoutApplicationNotificationData>
}

class NotificationForMissingOAuth2Application extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    recommendedScopeAnnotation(detail: OAuth2ConfigWithoutApplicationNotificationData) {
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

    addOAuth2ApplicationPrompt(detail: OAuth2ConfigWithoutApplicationNotificationData) {
      var matchingApplications = detail.existingOAuth2Applications.filter(ea => ea.apiId === detail.requiredApiConfig.apiId);
      if (matchingApplications.length === 1) {
        const app = matchingApplications[0];
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onUpdateOAuth2Application.bind(this, detail, app)}>

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
              onClick={this.onNewOAuth2Application.bind(this, detail, detail.requiredApiConfig)}>

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

    onUpdateOAuth2Application(detail: OAuth2ConfigWithoutApplicationNotificationData, app: OAuth2ApplicationRef) {
      detail.onUpdateOAuth2Application(detail.requiredApiConfig.clone({
        config: app
      }));
    }

    onNewOAuth2Application(detail: OAuth2ConfigWithoutApplicationNotificationData, requiredOAuth2ApiConfig: RequiredOAuth2Application) {
      detail.onNewOAuth2Application(requiredOAuth2ApiConfig);
    }

    render() {
      var numRequiredApiConfigs = this.props.details.length;
      if (numRequiredApiConfigs === 1) {
        var detail = this.props.details[0];
        return (
          <span>
            <span>This skill needs to be configured to use the <b>{detail.name}</b> API {this.recommendedScopeAnnotation(detail)}.</span>
            {this.addOAuth2ApplicationPrompt(detail)}
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
                  {this.addOAuth2ApplicationPrompt(ea)}
                  <span>{index + 1 < numRequiredApiConfigs ? ", " : ""}</span>
                </span>
              );
            })}
          </span>
        );
      }
    }
}

export default NotificationForMissingOAuth2Application;
