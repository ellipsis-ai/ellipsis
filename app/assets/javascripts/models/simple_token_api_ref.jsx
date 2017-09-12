define(function(require) {
  const ApiConfigRef = require('./api_config_ref');

  class SimpleTokenApiRef extends ApiConfigRef {

    static fromJson(props) {
      return new SimpleTokenApiRef(props);
    }

  }

  return SimpleTokenApiRef;
});
