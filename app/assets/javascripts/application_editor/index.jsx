define(function(require) {
  var React = require('react'),
    Collapsible = require('../collapsible'),
    Input = require('../form/input');

  return React.createClass({
    propTypes: {
      apis: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired
    },

    getInitialState: function() {
      return {
        applicationApi: null,
        applicationName: "",
        hasNamedApplication: false
      };
    },

    apiIsSet: function() {
      return !!this.state.applicationApi;
    },

    getApplicationApiName: function() {
      return this.state.applicationApi ? this.state.applicationApi.name : "";
    },

    setApplicationApi: function(api) {
      this.setState({ applicationApi: api });
    },

    resetApplicationApi: function() {
      this.setApplicationApi(null);
    },

    getApplicationName: function() {
      return this.state.applicationName;
    },

    applicationNameIsEmpty: function() {
      return !this.getApplicationName();
    },

    setApplicationName: function(name) {
      this.setState({ applicationName: name });
    },

    revealApplicationURL: function() {
      this.setState({ shouldRevealApplicationUrl: true });
    },

    shouldRevealApplicationUrl: function() {
      return this.state.shouldRevealApplicationUrl;
    },

    render: function() {
      if (this.apiIsSet()) {
        return this.renderConfigureApplication();
      } else {
        return this.renderChooseApi();
      }
    },

    renderChooseApi: function() {
      return (
        <div>
          <div className="bg-light">
            <div className="container pbm">
              <h3 className="mvn ptxxl type-weak display-ellipsis">
                <span className="mrs">API applications</span>
                <span className="mhs">→</span>
                <span className="mhs">Add an application</span>
              </h3>
            </div>
          </div>

          <div className="container">
            <div className="columns">
              <div className="column column-one-quarter">
              </div>
              <div className="column column-three-quarters bg-white pvxl phxxxl">
                <p>
                  <span>Choose an API you would like to integrate with Ellipsis. This will allow your behaviors to read </span>
                  <span>and/or write data from that product. You can create multiple applications for a single API, each </span>
                  <span>with a different level of access.</span>
                </p>

                <div className="mvxl">
                  {this.props.apis.map(function(api, index) {
                    return (
                      <button type="button" key={"apiTypeButton" + index} className="mrl mbl" onClick={this.setApplicationApi.bind(this, api)}>{api.name}</button>
                    );
                  }, this)}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderConfigureApplication: function() {
      return (
        <div>
          <div className="bg-light">
            <div className="container pbm">
              <h3 className="mvn ptxxl type-weak display-ellipsis">
                <span className="mrs">API applications</span>
                <span className="mhs">→</span>
                <span className="mhs"><button className="button-raw" onClick={this.resetApplicationApi}>Add an application</button></span>
                <span className="mhs">→</span>
                <span className="mhs">{this.getApplicationApiName()}</span>
              </h3>
            </div>
          </div>

          <div className="container">
            <div className="columns">
              <div className="column column-one-quarter">
              </div>
              <div className="column column-three-quarters bg-white pvxl phxxxl">
                <p>Configure a new {this.getApplicationApiName()} application so your behaviors can access data from a {this.getApplicationApiName()} account.</p>

                <div>
                  <h4 className="mbn position-relative">
                    <span className="position-hanging-indent">1.</span>
                    <span> Enter a name for this application</span>
                  </h4>
                  <p className="type-s">The name should help differentiate this from any other {this.getApplicationApiName()} applications you may have with different kinds of access, or access to a different set of data.</p>

                  <div className="mbxl">
                    <Input
                      value={this.getApplicationName()}
                      placeholder={"e.g. " + this.getApplicationApiName() + " read-only"}
                      className="form-input-borderless form-input-large"
                      onChange={this.setApplicationName}
                    />
                  </div>

                  <Collapsible revealWhen={!this.shouldRevealApplicationUrl()}>
                    <div className="mvxl">
                      <button type="button" disabled={this.applicationNameIsEmpty()} onClick={this.revealApplicationURL}>Continue</button>
                    </div>
                  </Collapsible>

                  <Collapsible revealWhen={this.shouldRevealApplicationUrl()}>
                    <hr className="mvxxxl" />

                    <div className="mvm">
                      <h4 className="mbn position-relative">
                        <span className="position-hanging-indent">2.</span>
                        <span>Register a new OAuth developer application on your GitHub account. </span>
                        <a href="https://github.com/settings/applications/new" target="_blank">Go to GitHub ↗︎</a>
                      </h4>
                      <ul className="type-s list-space-l mvl">
                        <li>You can set the name, homepage and description to whatever you like.</li>
                        <li>
                          <div>Copy and paste this for the authorization callback URL:</div>
                          <div className="box-code-example mtl">http://something.or.other/</div>
                        </li>
                      </ul>
                    </div>

                    <hr className="mvxxxl" />

                    <div className="mvm">
                      <h4 className="mbn position-relative">
                        <span className="position-hanging-indent">3.</span>
                        <span>Paste the Client ID and Client Secret from your {this.getApplicationApiName()} OAuth application</span>
                      </h4>
                      <p className="type-s">
                        These values will be generated by GitHub after you’ve saved an application there in step 2.
                      </p>

                      <div className="columns mtl">
                        <div className="column column-one-third">
                          <h5>Client ID</h5>
                          <Input className="form-input-borderless" placeholder="20-digit hexadecimal number" />
                        </div>
                        <div className="column column-one-third">
                          <h5>Client Secret</h5>
                          <Input className="form-input-borderless" placeholder="40-digit hexadecimal number" />
                        </div>
                      </div>
                    </div>
                  </Collapsible>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
  });
});
