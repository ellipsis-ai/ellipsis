define(function() {
  class RequiredApiConfig {
    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        apiId: { value: props.apiId, enumerable: true },
        nameInCode: { value: props.nameInCode, enumerable: true }
      });
    }

    canHaveConfig() {
      return false;
    }
  }

  return RequiredApiConfig;
});