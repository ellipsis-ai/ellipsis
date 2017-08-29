define(function(require) {
  var React = require('react'),
    AWSConfigRef = require('../models/aws_config_ref'),
    DropdownMenu = require('../shared_ui/dropdown_menu'),
    Input = require('../form/input'),
    RequiredAWSConfig = require('../models/required_aws_config'),
    Select = require('../form/select');

  return React.createClass({
    propTypes: {
      openWhen: React.PropTypes.bool.isRequired,
      allAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(AWSConfigRef)),
      requiredAWSConfigs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(RequiredAWSConfig)),
      allOAuth2Applications: React.PropTypes.arrayOf(React.PropTypes.shape({
        applicationId: React.PropTypes.string.isRequired,
        displayName: React.PropTypes.string.isRequired
      })).isRequired,
      requiredOAuth2ApiConfigs: React.PropTypes.arrayOf(React.PropTypes.shape({
        apiId: React.PropTypes.string.isRequired,
        recommendedScope: React.PropTypes.string,
        application: React.PropTypes.shape({
          applicationId: React.PropTypes.string.isRequired,
          displayName: React.PropTypes.string.isRequired
        })
      })).isRequired,
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

    getUsedAWSConfigIds: function() {
      return this.props.requiredAWSConfigs.filter(ea => !!ea.config).map(ea => ea.config.id);
    },

    getUnusedAWSConfigs: function() {
      const usedIds = this.getUsedAWSConfigIds();
      return this.props.allAWSConfigs.filter(ea => {
        return usedIds.indexOf(ea.id) === -1;
      });
    },

    addAWSConfig: function(cfg) {
      this.props.onAddAWSConfig(cfg);
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
                  {this.renderAdder()}
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
          menuClassName="popup-dropdown-menu-wide popup-dropdown-menu-left mobile-popup-dropdown-menu-left"
        >
          {this.getUnusedAWSConfigs().map((cfg, index) => {
            return (
              <DropdownMenu.Item
                key={"aws-config-" + index}
                onClick={this.addAWSConfig.bind(this, cfg)}
                label={this.getAWSSelectorLabelForConfig(cfg)}
              />
            );
          })}
          {/*{this.props.allOAuth2Applications.map((app, index) => {*/}
            {/*return (*/}
              {/*<DropdownMenu.Item*/}
                {/*key={"oauth2-app-" + index}*/}
                {/*checkedWhen={this.isRequiredOAuth2Application(app)}*/}
                {/*onClick={this.toggleOAuth2Application.bind(this, app)}*/}
                {/*label={this.getAPISelectorLabelForApp(app)}*/}
              {/*/>*/}
            {/*);*/}
          {/*})}*/}
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
          onChange={this.onConfigChange.bind(this, required)}
        >
          {this.props.allAWSConfigs.map(ea => <option value={ea.id}>{ea.displayName}</option>)}
        </Select>
      );
    },

    renderMissingAWSConfigFor: function(required) {
      return (
        <span>(missing)</span>
      );
    },

    updateRequiredConfig: function(oldRequired, newRequired) {
      this.props.onRemoveAWSConfig(oldRequired, () => {
        this.props.onAddAWSConfig(newRequired);
      });
    },

    onConfigChange: function(required, newConfigId) {
      const newConfig = this.props.allAWSConfigs.find(ea => ea.id === newConfigId);
      this.updateRequiredConfig(required, required.clone({
        config: newConfig
      }));
    },

    onNameInCodeChange: function(required, newNameInCode) {
      this.updateRequiredConfig(required, required.clone({
        nameInCode: newNameInCode
      }));
    },

    renderNameInCodeInputFor: function(required) {
      return (
        <Input
          className="form-input-borderless type-monospace"
          key={`requiredNameInCode${required.id}`}
          ref={`requiredNameInCode${required.id}`}
          value={required.nameInCode}
          placeholder="nameInCode"
          onChange={this.onNameInCodeChange.bind(this, required)}
        />
      );
    },

    renderRequiredAWSConfig: function(required) {
      return (
        <div>
          <div className="column">ellipsis.aws.</div>
          <div className="column">{this.renderNameInCodeInputFor(required)}</div>
          <div className="column">{required.config ? this.renderAWSConfigFor(required) : this.renderMissingAWSConfigFor(required)}</div>
        </div>
      );
    },

    renderAWSConfigs: function() {
      return (
        <div className="columns">
          {this.props.requiredAWSConfigs.map(this.renderRequiredAWSConfig)}
        </div>
      );
    }
  });

});
