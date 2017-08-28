define(function(require) {
  var React = require('react'),
    AWSConfigRef = require('../models/aws_config_ref'),
    DropdownMenu = require('../shared_ui/dropdown_menu'),
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
      toggle: React.PropTypes.func.isRequired
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
      })
    },

    addAWSConfig: function(cfg) {
      this.props.onAddAWSConfig(cfg);
    },

    render: function() {
      return (
        <div className="box-action phn">
          <div className="container">
            {this.renderAWSConfigs()}
            {this.renderAdder()}
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

    renderAWSConfig: function(config) {
      return (
        <span>{config.displayName}</span>
      );
    },

    renderMissingAWSConfigFor: function(required) {
      return (
        <span>(missing)</span>
      );
    },

    renderRequiredAWSConfig: function(required) {
      return (
        <div>
          <span>ellipsis.aws.{required.nameInCode}</span>
          {required.config ? this.renderAWSConfig(required.config) : this.renderMissingAWSConfigFor(required)}
        </div>
      );
    },

    renderAWSConfigs: function() {
      return (
        <div>
          {this.props.requiredAWSConfigs.map(this.renderRequiredAWSConfig)}
        </div>
      );
    }
  });

});
