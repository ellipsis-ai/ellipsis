define(function(require) {
  var React = require('react'),
    CSRFTokenHiddenInput = require('../shared_ui/csrf_token_hidden_input'),
    Input = require('../form/input'),
    Page = require('../shared_ui/page'),
    SettingsMenu = require('../shared_ui/settings_menu');

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
        <div className="flex-row-cascade">
          <div className="bg-light">
            <div className="container container-wide pbm">
              <h3 className="mvn ptxxl type-weak display-ellipsis">GitHub Configuration</h3>
            </div>
          </div>
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide prn">
              <div className="columns flex-columns flex-row-expand">
                <div className="column column-one-quarter flex-column">
                  <SettingsMenu activePage="githubConfig" teamId={this.props.teamId} isAdmin={this.props.isAdmin}/>
                </div>
                <div className="column column-three-quarters flex-column bg-white ptxxl pbxxxxl phxxxxl">

                  {this.getLinkedAccount() ? this.renderLinkedAccount() : this.renderNoLinkedAccount()}

                </div>
              </div>
            </div>
            <div className="flex-column flex-column-right bg-white" />
          </div>
          {this.props.onRenderFooter()}
        </div>
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
