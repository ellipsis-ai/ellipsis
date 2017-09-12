define(function(require) {
  var React = require('react'),
    ApiConfigRef = require('../models/api_config_ref'),
    DeleteButton = require('../shared_ui/delete_button'),
    DropdownMenu = require('../shared_ui/dropdown_menu'),
    Input = require('../form/input'),
    RequiredApiConfig = require('../models/required_api_config'),
    Select = require('../form/select');

  return React.createClass({
    propTypes: {
      openWhen: React.PropTypes.bool.isRequired,
      requiredConfig: React.PropTypes.instanceOf(RequiredApiConfig),
      allConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ApiConfigRef)),
      onAddConfig: React.PropTypes.func,
      onRemoveConfig: React.PropTypes.func,
      onUpdateConfig: React.PropTypes.func,
      toggle: React.PropTypes.func.isRequired,
      onDoneClick: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
      };
    },

    getApiId: function() {
      return this.props.renderConfig ? this.props.renderConfig.apiId : undefined;
    },

    getApiLogoUrl: function() {
      return this.props.requiredConfig ? this.props.requiredConfig.getApiLogoUrl() : undefined;
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

    getAllConfigs: function() {
      return this.sortedById(this.props.allConfigs);
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

    getSelectorLabelForConfig: function(config) {
      return config.getSelectorLabel();
    },

    onAddNewRequiredFor: function(config) {
      this.props.onAddConfig(config.newRequired())
    },

    render: function() {
      return (
        <div className="box-action phn">
          <div className="container">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h4 className="type-weak">Configure an API integration</h4>
              </div>
              <div className="column column-page-main">
                <div className="container pvl">
                  {this.renderConfig()}
                </div>
                <div className="ptxl">
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
          {this.props.allConfigs.map((cfg, index) => {
            return (
              <DropdownMenu.Item
                key={"api-config-" + index}
                onClick={this.onAddNewRequiredFor.bind(this, cfg)}
                label={this.getSelectorLabelForConfig(cfg)}
              />
            );
          })}
          {/*<DropdownMenu.Item*/}
            {/*onClick={this.addNewOAuth2Application}*/}
            {/*className="border-top"*/}
            {/*label="Add new API application…"*/}
          {/*/>*/}
        </DropdownMenu>
      );
    },

    renderConfigChoice: function() {
      const required = this.props.requiredConfig;
      return (
        <Select
          className="form-select-s form-select-light align-m mrm mbs"
          name="paramType"
          value={required.config ? required.config.id : undefined}
          onChange={this.onConfigChange}
        >
          <option key="config-choice-none" value={null}>None selected</option>
          {this.getAllConfigs().map(ea => <option key={`config-choice-${ea.id}`} value={ea.id}>{ea.displayName}</option>)}
        </Select>
      );
    },

    onConfigChange: function(newConfigId) {
      const newConfig = this.getAllConfigs().find(ea => ea.id === newConfigId);
      this.props.onUpdateConfig(this.props.requiredConfig.clone({
        config: newConfig
      }));
    },

    onNameInCodeChange: function(newNameInCode) {
      this.props.onUpdateConfig(this.props.requiredConfig.clone({
        nameInCode: newNameInCode
      }));
    },

    nameInCodeKey: function() {
      return `requiredNameInCode${this.props.requiredConfig.id}`;
    },

    renderNameInCodeInput: function() {
      return (
        <Input
          className="form-input-inline form-input-borderless type-monospace type-s type-bold"
          key={this.nameInCodeKey()}
          ref={this.nameInCodeKey()}
          value={this.props.requiredConfig.nameInCode}
          placeholder="nameInCode"
          onChange={this.onNameInCodeChange}
        />
      );
    },

    renderConfig: function() {
      if (this.props.requiredConfig) {
        return (
          <div>
            <div className="column"><img src={this.getApiLogoUrl()} height="32"/></div>
            <div className="column box-code-example mhs">
              <div className="columns">
                <div className="column type-monospace type-s pvs prn">ellipsis.aws.</div>
                <div className="column">
                  {this.renderNameInCodeInput()}
                </div>
              </div>
            </div>
            <div className="column pvs">{this.renderConfigChoice()}</div>
            {/*<div className="column column-shrink align-t">*/}
              {/*<DeleteButton onClick={onDeleteFn.bind(this, required)} />*/}
            {/*</div>*/}
          </div>
        );
      } else {
        return this.renderAdder();
      }
    }

  });

});
