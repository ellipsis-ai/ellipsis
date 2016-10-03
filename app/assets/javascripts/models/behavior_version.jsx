define(function(require) {
  var Param = require('./param'),
    ResponseTemplate = require('./response_template'),
    Trigger = require('./trigger');

  return {
    fromJson: function(props) {
      return Object.assign({}, props, {
        params: Param.paramsFromJson(props.params || []),
        responseTemplate: ResponseTemplate.fromString(props.responseTemplate || ''),
        triggers: Trigger.triggersFromJson(props.triggers || [])
      });
    }
  };
});
