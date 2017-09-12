define(function(require) {
  const AWSConfigRef = require('./aws_config_ref');
  const RequiredApiConfig = require('./required_api_config');

  class RequiredAWSConfig extends RequiredApiConfig {
    constructor(props) {
      super(props);
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        nameInCode: { value: props.nameInCode, enumerable: true },
        config: { value: props.config, enumerable: true }
      });
    }

    onAddConfigFor(editor) {
      return (required, callback) => {
        editor.onAddAWSConfig(required, callback);
      }
    }

    onRemoveConfigFor(editor) {
      return (required, callback) => {
        editor.onRemoveAWSConfig(required, callback);
      }
    }

    onUpdateConfigFor(editor) {
      return (required, callback) => {
        editor.onUpdateAWSConfig(required, callback);
      }
    }

    getApiLogoUrl() {
      return "/assets/images/logos/aws_logo_web_300px.png";
    }

    getAllConfigsFrom(editor) {
      return editor.getAllAWSConfigs();
    }

    codePath() {
      return `ellipsis.aws.${this.nameInCode}`
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

  return RequiredAWSConfig;
});
