define(function(require) {
  var Param = require('./param'),
    ResponseTemplate = require('./response_template'),
    Trigger = require('./trigger');

  class BehaviorVersion {
    constructor(props) {
      Object.defineProperties(this, {
        groupId: { value: props.groupId, enumerable: true },
        teamId: { value: props.teamId, enumerable: true },
        behaviorId: { value: props.behaviorId, enumerable: true },
        description: { value: props.description, enumerable: true },
        functionBody: { value: props.functionBody, enumerable: true },
        responseTemplate: { value: props.responseTemplate, enumerable: true },
        params: { value: props.params, enumerable: true },
        triggers: { value: props.triggers, enumerable: true },
        config: { value: props.config, enumerable: true },
        knownEnvVarsUsed: { value: props.knownEnvVarsUsed, enumerable: true },
        createdAt: { value: props.createdAt, enumerable: false },
        importedId: { value: props.importedId, enumerable: false }
      });
    }

    getDataTypeName() {
      return this.config.dataTypeName;
    }

    isDataType() {
      // empty string means it is a data type
      return !(this.getDataTypeName() === null || this.getDataTypeName() === undefined);
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

    isIdenticalToVersion(behaviorVersion) {
      return JSON.stringify(this) === JSON.stringify(behaviorVersion);
    }

    clone(props) {
      return new BehaviorVersion(Object.assign({}, this, props));
    }

    static fromJson(props) {
      return new BehaviorVersion(Object.assign({}, props, {
        params: Param.paramsFromJson(props.params || []),
        responseTemplate: ResponseTemplate.fromString(props.responseTemplate || ''),
        triggers: Trigger.triggersFromJson(props.triggers || [])
      }));
    }
  }

  return BehaviorVersion;
});
