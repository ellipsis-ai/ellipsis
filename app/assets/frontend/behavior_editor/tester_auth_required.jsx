import * as React from 'react';
import {RequiredOAuthApplication} from '../models/oauth';

const TesterAuthRequired = React.createClass({
    propTypes: {
      behaviorId: React.PropTypes.string.isRequired,
      oauthAppsRequiringAuth: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuthApplication)).isRequired
    },

    renderAuthRequiredFor: function (app) {
      const editUrl = jsRoutes.controllers.BehaviorEditorController.edit(this.props.behaviorId).absoluteURL(true);
      const url = jsRoutes.controllers.APIAccessController.linkCustomOAuth2Service(app.config.id, null, null, null, editUrl).absoluteURL(true);
      return (
        <a href={url}>{app.configName() || "(Unnamed)"}</a>
      );
    },

    render: function () {
      var apps = this.props.oauthAppsRequiringAuth;
      var numApps = apps.length;
      if (numApps === 1) {
        return (
          <div>
            <p>You need to authenticate with the following API in order to test: {this.renderAuthRequiredFor(apps[0])}</p>
          </div>
        );
      } else {
        return (
          <div>
            <p>You need to authenticate with the following APIs in order to test:</p>
            {apps.map((ea, index) => {
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
});

export default TesterAuthRequired;
