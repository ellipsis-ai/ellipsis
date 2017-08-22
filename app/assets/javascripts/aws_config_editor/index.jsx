define(function(require) {
  var React = require('react'),
    Collapsible = require('../shared_ui/collapsible'),
    CsrfTokenHiddenInput = require('../shared_ui/csrf_token_hidden_input'),
    SettingsMenu = require('../shared_ui/settings_menu'),
    ifPresent = require('../lib/if_present'),
    Input = require('../form/input'),
    Formatter = require('../lib/formatter');

  return React.createClass({
    displayName: 'ConfigEditor',
    propTypes: {
      configId: React.PropTypes.string,
      name: React.PropTypes.string,
      accessKeyId: React.PropTypes.string,
      secretAccessKey: React.PropTypes.string,
      region: React.PropTypes.string,
      configSaved: React.PropTypes.bool,
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      behaviorId: React.PropTypes.string,
      documentationUrl: React.PropTypes.string.isRequired,
      requiredAWSConfigId: React.PropTypes.string
    },

    getInitialState: function() {
      return {
        name: this.props.name || "",
        accessKeyId: this.props.accessKeyId || "",
        secretAccessKey: this.props.secretAccessKey || "",
        region: this.props.region || "",
        hasName: this.props.configSaved || false,
        isSaving: false
      };
    },

    getName: function() {
      return this.state.name;
    },

    nameIsEmpty: function() {
      return !this.getName();
    },

    setName: function(name) {
      this.setState({ name: name });
    },

    onNameEnterKey: function() {
      if (!this.nameIsEmpty()) {
        this.refs.configName.blur();
      }
    },

    getAccessKeyId: function() {
      return this.state.accessKeyId;
    },

    setAccessKeyId: function(value) {
      this.setState({ accessKeyId: value });
    },

    getSecretAccessKey: function() {
      return this.state.secretAccessKey;
    },

    setSecretAccessKey: function(value) {
      this.setState({ secretAccessKey: value });
    },

    getRegion: function() {
      return this.state.region;
    },

    setRegion: function(value) {
      this.setState({ region: value });
    },

    canBeSaved: function() {
      return !!(
        this.getName() && this.getAccessKeyId() && this.getSecretAccessKey() && this.getRegion()
      );
    },

    onSaveClick: function() {
      this.setState({
        isSaving: true
      });
    },

    onFocusExample: function(event) {
      if (event) {
        event.target.select();
      }
    },

    renderBehaviorId: function() {
      var id = this.props.behaviorId;
      if (id && id.length > 0) {
        return (<input type="hidden" name="behaviorId" value={id} />);
      } else {
        return null;
      }
    },

    render: function() {
      return (
        <form action={jsRoutes.controllers.AWSConfigController.save().url} method="POST">
          <CsrfTokenHiddenInput value={this.props.csrfToken} />
          <input type="hidden" name="requiredAWSConfigId" value={this.props.requiredAWSConfigId} />
          <input type="hidden" name="id" value={this.props.configId} />
          <input type="hidden" name="teamId" value={this.props.teamId} />
          {this.renderBehaviorId()}

          <div className="bg-light">
            <div className="container container-wide pbm">
              {this.renderHeader()}
            </div>
          </div>

          <div className="flex-columns">
            <div className="flex-column flex-column-left container container-wide prn">
              <div className="columns">
                <div className="column column-one-quarter">
                  <SettingsMenu activePage="awsConfigs" teamId={this.props.teamId} />
                </div>
                <div className="column column-three-quarters bg-white border-radius-bottom-left ptxl pbxxxxl phxxxxl">
                  {this.renderConfigure()}
                </div>
              </div>
            </div>
            <div className="flex-column flex-column-right bg-white"></div>
          </div>

          <footer className={
            "position-fixed-bottom position-z-front border-top ptm " +
            (this.canBeSaved() ? "bg-white" : "bg-light-translucent")
          }>
            <div className="container">
              <div className="columns mobile-columns-float">
                <div className="column column-one-quarter"></div>
                <div className="column column-three-quarters plxxxxl prm">
                  <button type="submit"
                          className={"button-primary mrs mbm " + (this.state.isSaving ? "button-activated" : "")}
                          disabled={!this.canBeSaved()}
                          onClick={this.onSaveClick}
                  >
                    <span className="button-labels">
                      <span className="button-normal-label">
                        <span className="mobile-display-none">Save changes</span>
                        <span className="mobile-display-only">Save</span>
                      </span>
                      <span className="button-activated-label">Saving…</span>
                    </span>
                  </button>
                </div>
              </div>
            </div>
          </footer>
        </form>
      );
    },

    renderHeader: function() {
      return (
        <h3 className="mvn ptxxl type-weak display-ellipsis">
          <span className="mrs">
            <a href={jsRoutes.controllers.AWSConfigController.list().url}>AWS configurations</a>
          </span>
          <span className="mhs">→</span>
          {this.renderApplicationHeader()}
        </h3>
      );
    },

    renderHeader: function() {
      if (!this.props.configSaved) {
        return (
          <span>
            <span className="mhs">→</span>
            <span className="mhs">{this.getName()}</span>
          </span>
        );
      } else {
        return (
          <span>
            <span className="mhs">Edit an AWS configuration</span>
            <span className="mhs">→</span>
            <span className="mhs">{this.getName() || (<span className="type-disabled">Untitled</span>)}</span>
          </span>
        );
      }
    },

    renderConfigure: function() {
      return (
        <div>
          <p className="mtm mbxl">Set up a new AWS configuration so your skills can access data from an AWS account.</p>

          <div>
            <h4 className="mbn position-relative">
              <span className="position-hanging-indent">1</span>
              <span> Enter a name for this configuration</span>
            </h4>
            <p className="type-s">The name should help differentiate this from any other AWS configurations you may have with different kinds of access, or access to a different set of data.</p>

            <div className="mbxxl columns">
              <div className="column column-two-thirds">
                <div>
                  <Input
                    ref="configName"
                    name="name"
                    value={this.getName()}
                    placeholder={"e.g. Default"}
                    className="form-input-borderless form-input-l type-l"
                    onChange={this.setName}
                    onEnterKey={this.onNameEnterKey}
                  />
                </div>

                <div className="mtm type-s">
                  <span className="type-weak mrxs">Name used in code: </span>
                  <code className="box-code-example">{Formatter.formatCamelCaseIdentifier(this.getName())}</code>
                </div>

              </div>
            </div>


            <Collapsible revealWhen={!this.nameIsEmpty()}>

              <div className="mvm">
                <h4 className="mbn position-relative">
                  <span className="position-hanging-indent">2</span>
                  <span>Ensure that you have a profile et up on your AWS account. </span>
                  {ifPresent(this.props.documentationUrl, url => (
                    <a href={url} target="_blank">Go to AWS ↗︎</a>
                  ))}
                </h4>
              </div>

              <hr className="mvxxxl" />

              <div className="mvm">
                <h4 className="mbn position-relative">
                  <span className="position-hanging-indent">3</span>
                  <span>Enter the configuration details</span>
                </h4>
                <p className="type-s">
                  The access keys will be generated by AWS after you’ve set up a profile.
                </p>

                <div className="mvm">
                  <div className="columns mtl">
                    <div className="column column-one-half">
                      <h5>Access Key ID</h5>
                      <Input className="form-input-borderless type-monospace"
                             placeholder="Enter access key ID"
                             name="accessKeyId"
                             value={this.getAccessKeyId()}
                             onChange={this.setAccessKeyId}
                             disableAuto={true}
                      />
                    </div>
                  </div>
                </div>
                <div className="mvm">
                  <div className="columns mtl">
                  <div className="column column-one-half">
                    <h5>Secret Access Key</h5>
                    <Input className="form-input-borderless type-monospace"
                           placeholder="Enter secret access key"
                           name="secretAccessKey"
                           value={this.getSecretAccessKey()}
                           onChange={this.setSecretAccessKey}
                           disableAuto={true}
                    />
                  </div>
                </div>
              </div>

              <div className="mvm">
                <div className="columns mtl">
                  <div className="column column-one-half">
                    <h5>Region</h5>
                    <Input className="form-input-borderless type-monospace"
                           placeholder="Enter the region"
                           name="region"
                           value={this.getRegion()}
                           onChange={this.setRegion}
                           disableAuto={true}
                    />
                  </div>
                </div>
              </div>
            </div>

            <hr className="mvxxxl" />

            </Collapsible>
          </div>
        </div>
      );
    }
  });
});
