define(function(require) {
  var React = require('react'),
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
        applicationName: ""
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

    setApplicationName: function(name) {
      this.setState({ applicationName: name });
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
                  <h4>Enter a name for this application</h4>
                  <p>The name should help differentiate this from any other {this.getApplicationApiName()} applications you may have with different kinds of access, or access to a different set of data.</p>

                  <Input
                    value={this.getApplicationName()}
                    placeholder={"e.g. " + this.getApplicationApiName() + " read-only"}
                    className="form-input-borderless form-input-large"
                    onChange={this.setApplicationName}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
  });
});
