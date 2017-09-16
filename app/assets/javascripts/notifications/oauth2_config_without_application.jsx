define(function(require) {
  var
    React = require('react'),
    oauth2 = require('../models/oauth2'),
    OAuth2ApplicationRef = oauth2.OAuth2ApplicationRef,
    RequiredOAuth2Application = oauth2.RequiredOAuth2Application;

  return React.createClass({
    displayName: 'NotificationForMissingOAuth2Application',
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        existingOAuth2Applications: React.PropTypes.arrayOf(React.PropTypes.instanceOf(OAuth2ApplicationRef)).isRequired,
        requiredApiConfig: React.PropTypes.instanceOf(RequiredOAuth2Application).isRequired,
        onUpdateOAuth2Application: React.PropTypes.func.isRequired,
        onNewOAuth2Application: React.PropTypes.func.isRequired,
        onConfigClick: React.PropTypes.func.isRequired
      })).isRequired
    },

    recommendedScopeAnnotation: function(detail) {
      var recommendedScope = detail.requiredApiConfig.recommendedScope;
      if (recommendedScope) {
        return (
          <span>(recommended scope: <b>{recommendedScope}</b>)</span>
        );
      }
    },

    addOAuth2ApplicationPrompt: function(detail) {
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
    },

    onUpdateOAuth2Application: function(detail, app) {
      detail.onUpdateOAuth2Application(detail.requiredApiConfig.clone({
        config: app
      }));
    },

    onNewOAuth2Application: function(detail, requiredOAuth2ApiConfig) {
      detail.onNewOAuth2Application(requiredOAuth2ApiConfig);
    },

    onConfigureOAuth2Application: function(detail, app) {

    },

    render: function() {
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
  });
});
