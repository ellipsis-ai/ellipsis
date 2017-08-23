define(function() {
  class RequiredAWSConfig {
    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        nameInCode: { value: props.nameInCode, enumerable: true }
      });
    }

    clone(props) {
      return new RequiredAWSConfig((Object.assign({}, this, props)));
    }

    static fromJson(props) {
      return new RequiredAWSConfig(props);
    }
  }

  return RequiredAWSConfig;
});
