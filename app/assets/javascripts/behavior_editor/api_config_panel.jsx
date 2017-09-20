define(function(require) {
  var React = require('react'),
    ifPresent = require('../lib/if_present'),
    ApiConfigRef = require('../models/api_config_ref'),
    DropdownMenu = require('../shared_ui/dropdown_menu'),
    Collapsible = require('../shared_ui/collapsible'),
    FormInput = require('../form/input'),
    Button = require('../form/button'),
    RequiredApiConfig = require('../models/required_api_config'),
    Select = require('../form/select');

  const ApiConfigPanel = React.createClass({
    propTypes: {
      openWhen: React.PropTypes.bool.isRequired,
      requiredConfig: React.PropTypes.instanceOf(RequiredApiConfig),
      allConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(ApiConfigRef)),
      onAddConfig: React.PropTypes.func,
      onAddNewConfig: React.PropTypes.func,
      onRemoveConfig: React.PropTypes.func,
      onUpdateConfig: React.PropTypes.func,
      getApiLogoUrlForRequired: React.PropTypes.func,
      getApiLogoUrlForConfig: React.PropTypes.func,
      getApiNameForConfig: React.PropTypes.func,
      toggle: React.PropTypes.func.isRequired,
      onDoneClick: React.PropTypes.func.isRequired,
      addNewAWSConfig: React.PropTypes.func.isRequired,
      addNewOAuth2Application: React.PropTypes.func.isRequired
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
      if (this.props.requiredConfig) {
        return this.props.getApiLogoUrlForRequired(this.props.requiredConfig);
      } else {
        return null;
      }
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
                <h4 className="type-weak mtn">Configure an API integration</h4>
              </div>
              <div className="column column-page-main">
                {this.renderConfig()}

                {this.renderButtons()}
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
          label="Select an API to use…"
          labelClassName="button-dropdown-trigger-menu-above"
          menuClassName="popup-dropdown-menu-wide popup-dropdown-menu-above"
        >
          {this.props.allConfigs.map((cfg, index) => (
            <DropdownMenu.Item
              key={"api-config-" + index}
              onClick={this.onAddNewRequiredFor.bind(this, cfg)}
              label={this.getSelectorLabelForConfig(cfg)}
            />
          ))}
          <DropdownMenu.Item
            onClick={this.props.addNewAWSConfig}
            className="border-top"
            label="Add new AWS configuration…"
          />
          <DropdownMenu.Item
            onClick={this.props.addNewOAuth2Application}
            label="Add new OAuth2 API application…"
          />
        </DropdownMenu>
      );
    },

    renderConfigChoice: function() {
      const required = this.props.requiredConfig;
      if (required.canHaveConfig()) {
        return (
          <div>
            <h5 className="mtn position-relative">
              <span>Set API configuration to use</span>
            </h5>
            <p className="type-s">These configurations can be shared across skills.</p>
            <div>
              <Select
                className="form-select-s form-select-light align-m mrm mbs"
                name="paramType"
                value={required.config ? required.config.id : undefined}
                onChange={this.onConfigChange}
              >
                <option key="config-choice-none" value={null}>None selected</option>
                {this.getAllConfigs().map(ea => (
                  <option key={`config-choice-${ea.id}`} value={ea.id}>{ea.displayName}</option>
                ))}
                <option key="config-choice-new" value={this.ADD_NEW_CONFIG_KEY}>Add a new configuration…</option>
              </Select>
            </div>
          </div>
        );
      }
    },

    ADD_NEW_CONFIG_KEY: "add_new_config",

    onConfigChange: function(newConfigId) {
      if (newConfigId === this.ADD_NEW_CONFIG_KEY) {
        this.props.onAddNewConfig(this.props.requiredConfig);
      } else {
        const newConfig = this.getAllConfigs().find(ea => ea.id === newConfigId);
        this.props.onUpdateConfig(this.props.requiredConfig.clone({
          config: newConfig
        }));
      }
    },

    onNameInCodeChange: function(newNameInCode) {
      this.props.onUpdateConfig(this.props.requiredConfig.clone({
        nameInCode: newNameInCode
      }));
    },

    renderNameInCode: function() {
      return (
        <div className="type-s">
          <h5 className="mtn position-relative"><span>Set code path</span></h5>
          <p className="mbxs">This determines how to access the configuration from code. It should be different from
            any other API integration in this skill.</p>

          <div>
            <div className="align-form-input display-inline-block">
              <span className="type-monospace">{this.props.requiredConfig.codePathPrefix()}</span>
            </div>
            <div className="align-form-input display-inline-block width-20">
              <FormInput
                className="form-input-borderless type-monospace"
                value={this.props.requiredConfig.nameInCode}
                placeholder="nameInCode"
                onChange={this.onNameInCodeChange}
              />
            </div>
          </div>
        </div>
      );
    },

    renderConfig: function() {
      const imageUrl = this.getApiLogoUrlForRequired();
      return (
        <div>
          <Collapsible revealWhen={Boolean(this.props.requiredConfig)}>

            {this.props.requiredConfig ? (
              <div>
                <div className="mbxl">
                  {imageUrl ? <img className="mrs align-m" src={imageUrl} height="24"/> : null}
                  <span>{this.props.getApiNameForConfig(this.props.requiredConfig)}</span>
                </div>

                <div className="mvxl">
                  {this.renderNameInCode()}
                </div>

                <div className="mvxl">
                  {this.renderConfigChoice()}
                </div>
              </div>
            ) : (
              <div/>
            )}
          </Collapsible>

          <Collapsible revealWhen={!this.props.requiredConfig}>
            {this.renderAdder()}
          </Collapsible>
        </div>
      );
    },

    renderButtons: function() {
      return (
        <div className="mtxl">
          <Button className="button-primary mbs mrs" onClick={this.props.onDoneClick}>
            {this.props.requiredConfig ? "Done" : "Cancel"}
          </Button>
          {this.props.requiredConfig ? (
            <Button className="button mbs" onClick={this.onDeleteRequired}>Remove integration from skill</Button>
          ) : null}
        </div>
      );
    }




  });

  return ApiConfigPanel;
});
