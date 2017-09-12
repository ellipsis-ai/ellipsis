define(function(require) {
  var React = require('react'),
    RequiredOAuth2Application = require('../models/oauth2').RequiredOAuth2Application;

  return React.createClass({
    propTypes: {
      behaviorId: React.PropTypes.string.isRequired,
      appsRequiringAuth: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth2Application)).isRequired
    },

    renderAuthRequiredFor: function (app) {
      return (
        <a
          href={jsRoutes.controllers.APIAccessController.linkCustomOAuth2Service(app.applicationId, null, null, null, jsRoutes.controllers.BehaviorEditorController.edit(this.props.behaviorId).url).url}>{app.displayName}</a>
      );
    },

    render: function () {
      var apps = this.props.appsRequiringAuth;
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
                <span className="phxs">
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

});
