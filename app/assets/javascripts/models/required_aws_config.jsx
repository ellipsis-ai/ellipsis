define(function(require) {
  const AWSConfigRef = require('./aws_config_ref');
  const RequiredApiConfig = require('./required_api_config');

  class RequiredAWSConfig extends RequiredApiConfig {
    constructor(props) {
      super(props);
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        nameInCode: { value: props.nameInCode, enumerable: true },
        config: { value: props.config, enumerable: true },
        attachedConfig: { value: props.config, enumerable: true }
      });
    }

    onAddConfigFor(editor) {
      return editor.onAddAWSConfig.bind(editor, this);
    }

    onRemoveConfigFor(editor) {
      return editor.onRemoveAWSConfig.bind(editor, this);
    }

    getApiLogoUrl() {
      return "/assets/images/logos/aws_logo_web_300px.png";
    }

    getAllConfigsFrom(editor) {
      return editor.getAllAWSConfigs();
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
