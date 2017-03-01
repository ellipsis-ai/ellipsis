define(function(require) {
  var Param = require('./param'),
    ResponseTemplate = require('./response_template'),
    Trigger = require('./trigger');

  class BehaviorVersion {
    constructor(props) {
      var initialTriggerProps = (props.config && props.config.dataTypeName) ? [] : [{}];
      var initialProps = Object.assign({
        functionBody: '',
        triggers: initialTriggerProps.map(ea => new Trigger(ea)),
        config: {},
        knownEnvVarsUsed: []
      }, props);

      Object.defineProperties(this, {
        groupId: { value: initialProps.groupId, enumerable: true },
        teamId: { value: initialProps.teamId, enumerable: true },
        behaviorId: { value: initialProps.behaviorId, enumerable: true },
        name: { value: initialProps.name, enumerable: true },
        description: { value: initialProps.description, enumerable: true },
        functionBody: { value: initialProps.functionBody, enumerable: true },
        responseTemplate: { value: initialProps.responseTemplate, enumerable: true },
        params: { value: initialProps.params, enumerable: true },
        triggers: { value: initialProps.triggers, enumerable: true },
        config: { value: initialProps.config, enumerable: true },
        knownEnvVarsUsed: { value: initialProps.knownEnvVarsUsed, enumerable: true },
        createdAt: { value: initialProps.createdAt, enumerable: false },
        exportId: { value: initialProps.exportId, enumerable: false }
      });
    }

    getDataTypeName() {
      return this.config.dataTypeName;
    }

    isDataType() {
      // empty string means it is a data type
      return !(this.getDataTypeName() === null || this.getDataTypeName() === undefined);
    }

    getTriggers() {
      return this.triggers || [];
    }

    findFirstTriggerIndexForDisplay() {
      var firstTriggerIndex = this.getTriggers().findIndex(function(trigger) {
        return !!trigger.text && !trigger.isRegex;
      });
      if (firstTriggerIndex === -1) {
        firstTriggerIndex = 0;
      }
      return firstTriggerIndex;
    }

    getFirstTriggerText() {
      var trigger = this.getTriggers()[this.findFirstTriggerIndexForDisplay()];
      if (trigger) {
        return trigger.text;
      } else {
        return "";
      }
    }

    isIdenticalToVersion(behaviorVersion) {
      return JSON.stringify(this) === JSON.stringify(behaviorVersion);
    }

    get sortKey() {
      return this.name || this.getFirstTriggerText();
    }

    clone(props) {
      return new BehaviorVersion(Object.assign({}, this, props));
    }

    static fromJson(props) {
      const materializedProps = Object.assign({}, props, {
        params: Param.paramsFromJson(props.params || []),
        responseTemplate: ResponseTemplate.fromString(props.responseTemplate || '')
      });
      if (props.triggers) {
        materializedProps.triggers = Trigger.triggersFromJson(props.triggers);
      }
      return new BehaviorVersion(materializedProps);
    }
  }

  return BehaviorVersion;
});
