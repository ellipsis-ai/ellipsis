define(function(require) {
  const ApiConfigRef = require('./api_config_ref');
  const RequiredApiConfig = require('./required_api_config');

  class AWSConfigRef extends ApiConfigRef {

    newRequired() {
      return new RequiredAWSConfig({
        apiId: 'aws',
        nameInCode: this.nameInCode,
        config: this
      });
    }

    static fromJson(props) {
      return new AWSConfigRef(props);
    }
  }

  class RequiredAWSConfig extends RequiredApiConfig {

    onAddConfigFor(editor) {
      return editor.onAddAWSConfig;
    }

    onRemoveConfigFor(editor) {
      return editor.onRemoveAWSConfig;
    }

    onUpdateConfigFor(editor) {
      return editor.onUpdateAWSConfig;
    }

    getApiLogoUrl() {
      return "/assets/images/logos/aws_logo_web_300px.png";
    }

    getAllConfigsFrom(editor) {
      return editor.getAllAWSConfigs();
    }

    codePathPrefix() {
      return "ellipsis.aws.";
    }

    codePath() {
      return `${this.codePathPrefix()}${this.nameInCode}`
    }

    configString() {
      if (this.config) {
        return `using: ${this.config.displayName}`;
      } else {
        return "not yet configured"
      }
    }

    clone(props) {
      return new RequiredAWSConfig((Object.assign({}, this, props)));
    }

    static fromJson(props) {
      return new RequiredAWSConfig(Object.assign({}, props, {
        config: props.config ? AWSConfigRef.fromJson(props.config) : undefined
      }));
    }
  }

  return {
    'AWSConfigRef': AWSConfigRef,
    'RequiredAWSConfig': RequiredAWSConfig
  };
});
