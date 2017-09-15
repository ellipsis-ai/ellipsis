define(function(require) {
  const RequiredApiConfig = require('./required_api_config');

  class RequiredApiConfigWithConfig extends RequiredApiConfig {
    constructor(props) {
      super(props);
      Object.defineProperties(this, {
        config: {value: props.config, enumerable: true}
      });
    }

    canHaveConfig() {
      return true;
    }
  }

  return RequiredApiConfigWithConfig;

});
