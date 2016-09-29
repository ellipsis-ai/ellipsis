define(function(require) {
  var ResponseTemplate = require('./response_template'),
    Trigger = require('./trigger');

  return {
    fromJson: function(props) {
      return Object.assign({}, props, {
        // responseTemplate: ResponseTemplate.fromString(props.responseTemplate),
        triggers: Trigger.triggersFromJson(props.triggers)
      });
    }
  };
});
