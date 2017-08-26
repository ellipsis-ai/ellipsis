define(function(require) {
  const AWSConfigRef = require('./aws_config_ref');
  class RequiredAWSConfig {
    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        nameInCode: { value: props.nameInCode, enumerable: true },
        config: { value: props.config, enumerable: true }
      });
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
