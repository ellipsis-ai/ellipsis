// @flow
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

    getApiLogoUrl(): string {
      return logoUrl;
    }

    getApiName(): string {
      return "AWS";
    }

    getAllConfigsFrom(editor) {
      return editor.getAllAWSConfigs();
    }

    codePathPrefix(): string {
      return "ellipsis.aws.";
    }

    codePath(): string {
      return `${this.codePathPrefix()}${this.nameInCode}`;
    }

    configName(): string {
      return this.config ? this.config.displayName : "";
    }

    isConfigured(): boolean {
      return Boolean(this.config);
    }

    clone(props): RequiredAWSConfig {
      return new RequiredAWSConfig((Object.assign({}, this, props)));
    }

  }

  class AWSConfigRef extends ApiConfigRef {

    newRequired(): RequiredAWSConfig {
      return new RequiredAWSConfig({
        id: ID.next(),
        apiId: 'aws',
        nameInCode: this.defaultNameInCode(),
        config: this
      });
    }

    getApiLogoUrl(): string {
      return logoUrl;
    }

    getApiName(editor): string {
      return editor.getOAuth2ApiNameForConfig(this);
    }

    configName(): string {
      return this.displayName;
    }

    static fromJson(props): AWSConfigRef {
      return new AWSConfigRef(props);
    }
  }

  RequiredAWSConfig.fromJson = function(props): RequiredAWSConfig {
    return new RequiredAWSConfig(Object.assign({}, props, {
      config: props.config ? AWSConfigRef.fromJson(props.config) : undefined
    }));
  };

  return {
    'AWSConfigRef': AWSConfigRef,
    'RequiredAWSConfig': RequiredAWSConfig
  };
});
