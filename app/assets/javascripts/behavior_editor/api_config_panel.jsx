define(function(require) {
  var React = require('react'),
    AWSConfigRef = require('../models/aws_config_ref'),
    DeleteButton = require('../shared_ui/delete_button'),
    DropdownMenu = require('../shared_ui/dropdown_menu'),
    Input = require('../form/input'),
    OAuth2ApplicationRef = require('../models/oauth2_application_ref'),
    RequiredAWSConfig = require('../models/required_aws_config'),
    RequiredOAuth2Application = require('../models/required_oauth2_application'),
    Select = require('../form/select');

  return React.createClass({
    propTypes: {
      openWhen: React.PropTypes.bool.isRequired,
      allAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(AWSConfigRef)),
      requiredAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredAWSConfig)),
      allOAuth2Applications: React.PropTypes.arrayOf(React.PropTypes.instanceOf(OAuth2ApplicationRef)).isRequired,
      requiredOAuth2Applications: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredOAuth2Application)).isRequired,
      allSimpleTokenApis: React.PropTypes.arrayOf(React.PropTypes.shape({
        apiId: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired
      })).isRequired,
      requiredSimpleTokenApis: React.PropTypes.arrayOf(React.PropTypes.shape({
        apiId: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired
      })).isRequired,
      onAddAWSConfig: React.PropTypes.func.isRequired,
      onRemoveAWSConfig: React.PropTypes.func.isRequired,
      onAddOAuth2Application: React.PropTypes.func.isRequired,
      onRemoveOAuth2Application: React.PropTypes.func.isRequired,
      onAddSimpleTokenApi: React.PropTypes.func.isRequired,
      onRemoveSimpleTokenApi: React.PropTypes.func.isRequired,
      onNewOAuth2Application: React.PropTypes.func.isRequired,
      getOAuth2ApiWithId: React.PropTypes.func.isRequired,
      toggle: React.PropTypes.func.isRequired,
      onDoneClick: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
      };
    },


    getAWSSelectorLabelForConfig: function(cfg) {
      return (
        <div className="columns columns-elastic">
          <div className="column column-shrink prs align-m">
            <img src="/assets/images/logos/aws_logo_web_300px.png" height="32"/>
          </div>
          <div className="column column-expand align-m">
            {cfg.displayName}
          </div>
        </div>
      );
    },

    sortedById: function(arr) {
      return arr.sort((a, b) => {
        if (a.id === b.id) {
          return 0;
        } else if (a.id < b.id) {
          return -1;
        } else {
          return 1;
        }
      });
    },

    getSortedRequiredAWSConfigs: function() {
      return this.sortedById(this.props.requiredAWSConfigs);
    },

    getSortedRequiredOAuth2Applications: function() {
      return this.sortedById(this.props.requiredOAuth2Applications);
    },

    isRequiredOAuth2Application: function(app) {
      var appIndex = this.props.requiredOAuth2Applications.findIndex(function(ea) {
        return ea.application && ea.application.applicationId === app.applicationId;
      });
      return appIndex >= 0;
    },

    getAPISelectorLabelForApi: function(api, displayName) {
      if (api && api.iconImageUrl) {
        return (
          <div className="columns columns-elastic">
            <div className="column column-shrink prs align-m">
              <img src={api.iconImageUrl} width="24" height="24"/>
            </div>
            <div className="column column-expand align-m">
              {displayName}
            </div>
          </div>
        );
      } else if (api && api.logoImageUrl) {
        return (
          <div className="columns columns-elastic">
            <div className="column column-shrink prs align-m">
              <img src={api.logoImageUrl} height="18" />
            </div>
            <div className="column column-expand align-m">
              {displayName}
            </div>
          </div>
        );
      } else {
        return (
          <span>{displayName}</span>
        );
      }
    },

    getAPISelectorLabelForApp: function(app) {
      var api = this.props.getOAuth2ApiWithId(app.apiId);
      return this.getAPISelectorLabelForApi(api, app.displayName);
    },

    render: function() {
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak">Third-party APIs used in this skill</h4>
              </div>
              <div className="column column-page-main">
                <div className="container pvl">
                  {this.renderAWSConfigs()}
                  {this.renderOAuth2Applications()}
                </div>
                <div className="ptxl">
                  {this.renderAdder()}
                  <button className="button-primary mbs" type="button" onClick={this.props.onDoneClick}>Done</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderAdder: function() {
      return (
        <DropdownMenu
          openWhen={this.props.openWhen}
          toggle={this.props.toggle}
          label="Add an API"
          labelClassName="button-s"
          menuClassName="popup-dropdown-menu-wide popup-dropdown-menu-left mobile-popup-dropdown-menu-left popup-dropdown-menu-above"
        >
          {this.props.allAWSConfigs.map((cfg, index) => {
            return (
              <DropdownMenu.Item
                key={"aws-config-" + index}
                onClick={this.onAddAWSConfig.bind(this, cfg)}
                label={this.getAWSSelectorLabelForConfig(cfg)}
              />
            );
          })}
          {this.props.allOAuth2Applications.map((app, index) => {
            return (
              <DropdownMenu.Item
                key={"oauth2-app-" + index}
                onClick={this.onAddOAuth2Application.bind(this, app)}
                label={this.getAPISelectorLabelForApp(app)}
              />
            );
          })}
          {/*{this.props.allSimpleTokenApis.map((api, index) => {*/}
            {/*return (*/}
              {/*<DropdownMenu.Item*/}
                {/*key={"simple-token-api-" + index}*/}
                {/*checkedWhen={this.isRequiredSimpleTokenApi(api)}*/}
                {/*onClick={this.toggleSimpleTokenApi.bind(this, api)}*/}
                {/*label={this.getAPISelectorLabelForApi(api, api.name)}*/}
              {/*/>*/}
            {/*);*/}
          {/*})}*/}
          {/*<DropdownMenu.Item*/}
            {/*onClick={this.addNewOAuth2Application}*/}
            {/*className="border-top"*/}
            {/*label="Add new API application…"*/}
          {/*/>*/}
        </DropdownMenu>
      );
    },

    renderAWSConfigFor: function(required) {
      return (
        <Select
          className="form-select-s form-select-light align-m mrm mbs"
          name="paramType"
          value={required.config ? required.config.id : null}
          onChange={this.onAWSConfigChange.bind(this, required)}
        >
          <option value={null}>None selected</option>
          {this.props.allAWSConfigs.map(ea => <option value={ea.id}>{ea.displayName}</option>)}
        </Select>
      );
    },

    getOAuth2ApplicationsFor: function(apiId) {
      return this.props.allOAuth2Applications.filter(ea => ea.apiId === apiId);
    },

    renderOAuth2ApplicationFor: function(required) {
      return (
        <Select
          className="form-select-s form-select-light align-m mrm mbs"
          name="paramType"
          value={required.application ? required.application.applicationId : null}
          onChange={this.onOAuth2ApplicationChange.bind(this, required)}
        >
          <option value={null}>None selected</option>
          {this.getOAuth2ApplicationsFor(required.apiId).map(ea => <option value={ea.applicationId}>{ea.displayName}</option>)}
        </Select>
      );
    },

    updateRequiredAWSConfig: function(oldRequired, newRequired) {
      this.props.onRemoveAWSConfig(oldRequired, () => {
        this.props.onAddAWSConfig(newRequired, () => {
          if (oldRequired.nameInCode !== newRequired.nameInCode) {
            const input = this.refs[this.nameInCodeKeyFor(newRequired)];
            input.focus();
            input.refs.input.selectionStart = input.props.value.length;
            input.refs.input.selectionEnd = input.props.value.length;
          }
        });
      });
    },

    onAWSConfigChange: function(required, newConfigId) {
      const newConfig = this.props.allAWSConfigs.find(ea => ea.id === newConfigId);
      this.updateRequiredAWSConfig(required, required.clone({
        config: newConfig
      }));
    },

    updateRequiredOAuth2Application: function(oldRequired, newRequired) {
      this.props.onRemoveOAuth2Application(oldRequired, () => {
        this.props.onAddOAuth2Application(newRequired, () => {
          if (oldRequired.nameInCode !== newRequired.nameInCode) {
            const input = this.refs[this.nameInCodeKeyFor(newRequired)];
            input.focus();
            input.refs.input.selectionStart = input.props.value.length;
            input.refs.input.selectionEnd = input.props.value.length;
          }
        });
      });
    },

    onOAuth2ApplicationChange: function(required, newApplicationId) {
      const newApplication = this.props.allOAuth2Applications.find(ea => ea.applicationId === newApplicationId);
      this.updateRequiredOAuth2Application(required, required.clone({
        application: newApplication
      }));
    },

    onNameInCodeChange: function(required, newNameInCode) {
      this.updateRequiredAWSConfig(required, required.clone({
        nameInCode: newNameInCode
      }));
    },

    onDeleteAWSConfig: function(required) {
      this.props.onRemoveAWSConfig(required);
    },

    onAddAWSConfig: function(config) {
      this.props.onAddAWSConfig(new RequiredAWSConfig({
        nameInCode: config.nameInCode,
        config: config
      }));
    },

    onAddOAuth2Application: function(app) {
      this.props.onAddOAuth2Application(new RequiredOAuth2Application({
        apiId: app.apiId,
        recommendedScope: app.scope,
        nameInCode: app.nameInCode,
        application: app
      }))
    },

    onDeleteRequiredOAuth2Application: function(required) {
      this.props.onRemoveOAuth2Application(required);
    },

    nameInCodeKeyFor: function(required) {
      return `requiredNameInCode${required.id}`;
    },

    renderNameInCodeInputFor: function(required) {
      return (
        <Input
          className="form-input-inline form-input-borderless type-monospace type-s type-bold"
          key={this.nameInCodeKeyFor(required)}
          ref={this.nameInCodeKeyFor(required)}
          value={required.nameInCode}
          placeholder="nameInCode"
          onChange={this.onNameInCodeChange.bind(this, required)}
        />
      );
    },

    renderRequiredFor: function(required, apiLogoUrl, configPart, onDeleteFn) {
      return (
        <div>
          <div className="column"><img src={apiLogoUrl} height="32"/></div>
          <div className="column box-code-example mhs">
            <div className="columns">
              <div className="column type-monospace type-s pvs prn">ellipsis.aws.</div>
              <div className="column">
                {this.renderNameInCodeInputFor(required)}
              </div>
            </div>
          </div>
          <div className="column pvs">{configPart}</div>
          <div className="column column-shrink align-t">
            <DeleteButton onClick={onDeleteFn.bind(this, required)} />
          </div>
        </div>
      );
    },

    renderRequiredAWSConfig: function(required) {
      return this.renderRequiredFor(required, "/assets/images/logos/aws_logo_web_300px.png", this.renderAWSConfigFor(required), this.onDeleteAWSConfig);
    },

    renderRequiredOAuth2Application: function(required) {
      const api = this.props.getOAuth2ApiWithId(required.apiId);
      const apiLogoUrl = (api && api.iconImageUrl) || (api && api.logoImageUrl);
      return this.renderRequiredFor(required, apiLogoUrl, this.renderOAuth2ApplicationFor(required), this.onDeleteRequiredOAuth2Application);
    },

    renderAWSConfigs: function() {
      return (
        <div className="columns">
          {this.getSortedRequiredAWSConfigs().map(this.renderRequiredAWSConfig)}
        </div>
      );
    },

    renderOAuth2Applications: function() {
      return (
        <div className="columns">
          {this.getSortedRequiredOAuth2Applications().map(this.renderRequiredOAuth2Application)}
        </div>
      );
    }

  });

});
