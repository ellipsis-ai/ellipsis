import * as React from 'react';
import {OAuthApplicationRef, RequiredOAuthApplication} from '../models/oauth';
import autobind from "../lib/autobind";
import ID from "../lib/id";

interface Props {
  groupId: string,
  behaviorId: string,
  appsRequiringAuth: Array<RequiredOAuthApplication>
}

class TesterAuthRequired extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  renderAuthRequiredFor(config: OAuthApplicationRef) {
      const editUrl = jsRoutes.controllers.BehaviorEditorController.edit(this.props.groupId, this.props.behaviorId).absoluteURL(true);
      const state = JSON.stringify({ oauthState: ID.next(), redirect: editUrl });
      const url = jsRoutes.controllers.APIAccessController.linkCustomOAuth2Service(config.id, null, state).absoluteURL(true);
      return (
        <a href={url}>{config.displayName || "(Unnamed)"}</a>
      );
  }

  render() {
      var appConfigs = this.props.appsRequiringAuth
        .map((ea) => ea.config)
        .filter<OAuthApplicationRef>((eaConfig): eaConfig is OAuthApplicationRef => Boolean(eaConfig));
      var numApps = appConfigs.length;
      if (numApps === 1) {
        return (
          <div>
            <p>You need to authenticate with the following API in order to test: {this.renderAuthRequiredFor(appConfigs[0])}</p>
          </div>
        );
      } else {
        return (
          <div>
            <p>You need to authenticate with the following APIs in order to test:</p>
            {appConfigs.map((ea, index) => {
              return (
                <span className="phxs" key={`auth-required-${index}`}>
                    <span>{this.renderAuthRequiredFor(ea)}</span>
                    <span>{index + 1 < numApps ? ", " : ""}</span>
                  </span>
              );
            })}
          </div>
        );
      }
  }
}

export default TesterAuthRequired;
