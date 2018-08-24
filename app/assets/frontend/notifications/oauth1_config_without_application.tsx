import * as React from 'react';
import {OAuth1ApplicationRef, RequiredOAuth1Application} from '../models/oauth1';
import OAuth1ConfigWithoutApplicationNotificationData from "../models/notifications/oauth1_config_without_application_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<OAuth1ConfigWithoutApplicationNotificationData>
}

class NotificationForMissingOAuth1Application extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  recommendedScopeAnnotation(detail: OAuth1ConfigWithoutApplicationNotificationData) {
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

  addOAuth1ApplicationPrompt(detail: OAuth1ConfigWithoutApplicationNotificationData) {
    var matchingApplications = detail.existingOAuth1Applications.filter(ea => ea.apiId === detail.requiredApiConfig.apiId);
    if (matchingApplications.length === 1) {
      const app = matchingApplications[0];
      return (
        <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onUpdateOAuth1Application.bind(this, detail, app)}>

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
              onClick={this.onNewOAuth1Application.bind(this, detail, detail.requiredApiConfig)}>

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

  onUpdateOAuth1Application(detail: OAuth1ConfigWithoutApplicationNotificationData, app: OAuth1ApplicationRef) {
    detail.onUpdateOAuth1Application(detail.requiredApiConfig.clone({
      config: app
    }));
  }

  onNewOAuth1Application(detail: OAuth1ConfigWithoutApplicationNotificationData, requiredOAuth1ApiConfig: RequiredOAuth1Application) {
    detail.onNewOAuth1Application(requiredOAuth1ApiConfig);
  }

  render() {
    var numRequiredApiConfigs = this.props.details.length;
    if (numRequiredApiConfigs === 1) {
      var detail = this.props.details[0];
      return (
        <span>
            <span>This skill needs to be configured to use the <b>{detail.name}</b> API {this.recommendedScopeAnnotation(detail)}.</span>
          {this.addOAuth1ApplicationPrompt(detail)}
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
                {this.addOAuth1ApplicationPrompt(ea)}
                <span>{index + 1 < numRequiredApiConfigs ? ", " : ""}</span>
                </span>
            );
          })}
          </span>
      );
    }
  }
}

export default NotificationForMissingOAuth1Application;
