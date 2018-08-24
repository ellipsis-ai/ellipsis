import * as React from 'react';
import {RequiredOAuth1Application} from '../models/oauth1';
import {RequiredOAuth2Application} from '../models/oauth2';

const TesterAuthRequired = React.createClass({
    propTypes: {
      behaviorId: React.PropTypes.string.isRequired,
      oauth1AppsRequiringAuth: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth1Application)).isRequired,
      oauth2AppsRequiringAuth: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth2Application)).isRequired
    },

    renderAuthRequiredFor: function (app) {
      const editUrl = jsRoutes.controllers.BehaviorEditorController.edit(this.props.behaviorId).absoluteURL(true);
      const url = jsRoutes.controllers.APIAccessController.linkCustomOAuth2Service(app.config.id, null, null, null, editUrl).absoluteURL(true);
      return (
        <a href={url}>{app.configName() || "(Unnamed)"}</a>
      );
    },

    render: function () {
      var apps = this.props.oauth1AppsRequiringAuth.concat(this.props.oauth2AppsRequiringAuth);
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
