import * as React from 'react';
import {OAuthApplicationRef, RequiredOAuthApplication} from '../models/oauth';
import autobind from "../lib/autobind";

interface Props {
  behaviorId: string,
  appsRequiringAuth: Array<RequiredOAuthApplication>
}

class TesterAuthRequired extends React.Component<Props> {
  constructor(props) {
    super(props);
    autobind(this);
  }

  renderAuthRequiredFor(config: OAuthApplicationRef) {
      const editUrl = jsRoutes.controllers.BehaviorEditorController.edit(this.props.behaviorId).absoluteURL(true);
      const url = jsRoutes.controllers.APIAccessController.linkCustomOAuth2Service(config.id, null, null, null, editUrl).absoluteURL(true);
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
