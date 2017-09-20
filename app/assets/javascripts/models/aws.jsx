define(function(require) {
  const ApiConfigRef = require('./api_config_ref');
  const RequiredApiConfigWithConfig = require('./required_api_config_with_config');
  const ID = require('../lib/id');

  const logoUrl = "/assets/images/logos/aws_logo_web_300px.png";

  class RequiredAWSConfig extends RequiredApiConfigWithConfig {

    onAddConfigFor(editor) {
      return editor.onAddAWSConfig;
    }

    onAddNewConfigFor(editor) {
      return editor.addNewAWSConfig;
    }

    onRemoveConfigFor(editor) {
      return editor.onRemoveAWSConfig;
    }

    onUpdateConfigFor(editor) {
      return editor.onUpdateAWSConfig;
    }

    getApiLogoUrl() {
      return () => logoUrl;
    }

    getApiName() {
      return "Amazon Web Services (AWS)";
    }

    getAllConfigsFrom(editor) {
      return editor.getAllAWSConfigs();
    }

    codePathPrefix() {
      return "ellipsis.aws.";
    }

    codePath() {
      return `${this.codePathPrefix()}${this.nameInCode}`;
    }

    configString() {
      if (this.config) {
        return this.config.displayName;
      } else {
        return "Not yet configured";
      }
    }

    clone(props) {
      return new RequiredAWSConfig((Object.assign({}, this, props)));
    }

  }

  class AWSConfigRef extends ApiConfigRef {

    newRequired() {
      return new RequiredAWSConfig({
        id: ID.next(),
        apiId: 'aws',
        nameInCode: this.defaultNameInCode(),
        config: this
      });
    }

    getApiLogoUrl() {
      return logoUrl;
    }

    static fromJson(props) {
      return new AWSConfigRef(props);
    }
  }

  RequiredAWSConfig.fromJson = function(props) {
    return new RequiredAWSConfig(Object.assign({}, props, {
      config: props.config ? AWSConfigRef.fromJson(props.config) : undefined
    }));
  };

  return {
    'AWSConfigRef': AWSConfigRef,
    'RequiredAWSConfig': RequiredAWSConfig
  };
});
