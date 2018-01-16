define(function(require) {
  var React = require('react'),
    CSRFTokenHiddenInput = require('../shared_ui/csrf_token_hidden_input'),
    Page = require('../shared_ui/page'),
    SettingsPage = require('../shared_ui/settings_page');

  const resetForm = jsRoutes.controllers.GithubConfigController.reset();

  const GithubConfig = React.createClass({
    propTypes: Object.assign({}, Page.requiredPropTypes, {
      isAdmin: React.PropTypes.bool.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      linkedAccount: React.PropTypes.shape({
        providerId: React.PropTypes.string.isRequired,
        providerKey: React.PropTypes.string.isRequired,
        createdAt: React.PropTypes.string.isRequired
      })
    }),

    getDefaultProps: function() {
      return Page.requiredPropDefaults();
    },

    getInitialState: function() {
      return {
        linkedAccount: this.props.linkedAccount
      };
    },

    getLinkedAccount: function() {
      return this.state.linkedAccount;
    },

    render: function() {
      return (
        <SettingsPage teamId={this.props.teamId} isAdmin={this.props.isAdmin} header={"GitHub Configuration"} activePage={"githubConfig"}>
          {this.getLinkedAccount() ? this.renderLinkedAccount() : this.renderNoLinkedAccount()}
          {this.props.onRenderFooter()}
        </SettingsPage>
      );
    },

    renderLogo: function() {
      return (
        <img height="32" src="/assets/images/logos/GitHub-Mark-64px.png"/>
      );
    },

    renderLinkedAccount: function() {
      return (
        <div>
          <div className="columns">
            <div className="column">
              {this.renderLogo()}
            </div>
            <div className="column">
              <span>You have linked to your GitHub account</span>
            </div>
            <div className="column">
              <form action={resetForm.url} method={resetForm.method}>
                <CSRFTokenHiddenInput value={this.props.csrfToken} />
                <button type="submit" className="button-s button-shrink">Reset</button>
              </form>
            </div>
          </div>
        </div>
      );
    },

    getGithubAuthUrl: function() {
      const redirect = jsRoutes.controllers.GithubConfigController.index().url;
      return jsRoutes.controllers.SocialAuthController.authenticateGithub(redirect).url;
    },

    renderNoLinkedAccount: function() {
      return (
        <div className="columns">
          <div className="column">
            {this.renderLogo()}
          </div>
          <div className="column align-m">
            <span>To push code to or pull code from GitHub, you first need to </span>
            <a href={this.getGithubAuthUrl()}>authenticate your GitHub account</a>
          </div>
        </div>
      );
    }

  });

  return GithubConfig;
});
