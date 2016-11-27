define(function(require) {
  var Param = require('./param'),
    ResponseTemplate = require('./response_template'),
    Trigger = require('./trigger');

  class BehaviorVersion {
    constructor(props) {
      Object.assign(this, props);
    }

    isDataType() {
      return !!this.dataTypeName;
    }

    findFirstTriggerIndexForDisplay() {
      var firstTriggerIndex = this.triggers.findIndex(function(trigger) {
        return !!trigger.text && !trigger.isRegex;
      });
      if (firstTriggerIndex === -1) {
        firstTriggerIndex = 0;
      }
      return firstTriggerIndex;
    }

    getFirstTriggerText() {
      var trigger = this.triggers[this.findFirstTriggerIndexForDisplay()];
      if (trigger) {
        return trigger.text;
      } else {
        return "";
      }
    }

    clone(props) {
      return new BehaviorVersion(Object.assign({}, this, props));
    }

    static fromJson(props) {
      return new BehaviorVersion(Object.assign({}, props, {
        params: Param.paramsFromJson(props.params || []),
        responseTemplate: ResponseTemplate.fromString(props.responseTemplate || ''),
        triggers: Trigger.triggersFromJson(props.triggers || []),
        dataTypeName: props.config.dataTypeName
      }));
    }
  }

  return BehaviorVersion;
});
