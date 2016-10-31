define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'NotificationForMissingOAuth2Application',
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        existingOAuth2Applications: React.PropTypes.arrayOf(React.PropTypes.shape({
          apiId: React.PropTypes.string.isRequired,
          applicationId: React.PropTypes.string.isRequired,
          displayName: React.PropTypes.string,
          keyName: React.PropTypes.string,
          scope: React.PropTypes.string
        })).isRequired,
        requiredApiConfig: React.PropTypes.shape({
          id: React.PropTypes.string.isRequired,
          apiId: React.PropTypes.string.isRequired,
          recommendedScope: React.PropTypes.string,
          application: React.PropTypes.shape({
            apiId: React.PropTypes.string.isRequired,
            applicationId: React.PropTypes.string.isRequired,
            displayName: React.PropTypes.string,
            keyName: React.PropTypes.string,
            scope: React.PropTypes.string
          })
        }).isRequired,
        onAddOAuth2Application: React.PropTypes.func.isRequired,
        onNewOAuth2Application: React.PropTypes.func.isRequired
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
      var matchingApplication = detail.existingOAuth2Applications.find(ea => ea.apiId === detail.requiredApiConfig.apiId);
      if (matchingApplication) {
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onAddOAuth2Application.bind(this, detail, matchingApplication)}>

              Add {matchingApplication.displayName} to this skill

            </button>
          </span>
        );
      } else {
        return (
          <span className="mhxs">
            <button
              type="button"
              className="button-raw link button-s"
              onClick={this.onNewOAuth2Application.bind(this, detail, detail.requiredApiConfig.id)}>

              Configure the {detail.name} API for this skill

            </button>
          </span>
        );
      }
    },

    onAddOAuth2Application: function(detail, app) {
      detail.onAddOAuth2Application(app);
    },

    onNewOAuth2Application: function(detail, requiredOAuth2ApiConfigId) {
      detail.onNewOAuth2Application(requiredOAuth2ApiConfigId);
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
