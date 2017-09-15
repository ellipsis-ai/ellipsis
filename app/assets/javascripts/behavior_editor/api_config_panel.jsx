define(function(require) {
  var React = require('react'),
    ifPresent = require('../lib/if_present'),
    ApiConfigRef = require('../models/api_config_ref'),
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
      getApiLogoUrlForRequired: React.PropTypes.func,
      getApiLogoUrlForConfig: React.PropTypes.func,
      toggle: React.PropTypes.func.isRequired,
      onDoneClick: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
      };
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

    getApiLogoUrlForRequired: function() {
      return this.props.getApiLogoUrlForRequired(this.props.requiredConfig);
    },

    getApiLogoUrlForConfig: function(config) {
      return this.props.getApiLogoUrlForConfig(config);
    },

    getSelectorLabelForConfig: function(config) {
      return (
        <div className="columns columns-elastic">
          {ifPresent(this.getApiLogoUrlForConfig(config), url => {
            return (
              <div className="column column-shrink prs align-m">
                <img src={url} height="24"/>
              </div>
            );
          })}
          <div className="column column-expand align-m">
             {config.getSelectorLabel()}
           </div>
         </div>
      );
    },

    onAddNewRequiredFor: function(config) {
      this.props.onAddConfig(config.newRequired());
    },

    onDeleteRequired: function() {
      this.props.onRemoveConfig(this.props.requiredConfig);
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
      if (required.canHaveConfig()) {
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
      } else {
        return null;
      }
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
            <div><img src={this.getApiLogoUrlForRequired()} height="32"/></div>
            <div className="box-code-example">
              <div className="columns">
                <div className="column type-monospace type-s pvs prn">{this.props.requiredConfig.codePathPrefix()}</div>
                <div className="column">
                  {this.renderNameInCodeInput()}
                </div>
              </div>
            </div>
            <div className="pvs">{this.renderConfigChoice()}</div>
            <div className="ptxl">
              <button className="button-primary mbs mrs" type="button" onClick={this.props.onDoneClick}>Done</button>
              <button className="button mbs" type="button" onClick={this.onDeleteRequired}>Remove from this skill</button>
            </div>
          </div>
        );
      } else {
        return this.renderAdder();
      }
    }

  });

});
