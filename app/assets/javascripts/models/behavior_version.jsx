define(function(require) {
  var DeepEqual = require('../lib/deep_equal'),
    Editable = require('./editable'),
    ResponseTemplate = require('./response_template'),
    Trigger = require('./trigger');

  class BehaviorVersion extends Editable {
    constructor(props) {
      super(props);

      var initialProps = Object.assign({
        config: {},
        knownEnvVarsUsed: [],
        shouldRevealCodeEditor: (!!props.functionBody && props.functionBody.length > 0),
        createdAt: Date.now()
      }, props);

      Object.defineProperties(this, {
        behaviorId: { value: initialProps.behaviorId, enumerable: true },
        isNewBehavior: { value: initialProps.isNewBehavior, enumerable: true },
        description: { value: initialProps.description, enumerable: true },
        responseTemplate: { value: initialProps.responseTemplate, enumerable: true },
        inputIds: { value: initialProps.inputIds, enumerable: true },
        triggers: { value: initialProps.triggers, enumerable: true },
        config: { value: initialProps.config, enumerable: true },
        knownEnvVarsUsed: { value: initialProps.knownEnvVarsUsed, enumerable: true },
        createdAt: { value: initialProps.createdAt, enumerable: true },
        exportId: { value: initialProps.exportId, enumerable: true },
        shouldRevealCodeEditor: { value: initialProps.shouldRevealCodeEditor, enumerable: true }
      });
    }

    buildUpdatedGroupFor(group, props) {
      const timestampedBehavior = this.clone(props).copyWithNewTimestamp();
      const updatedVersions = group.behaviorVersions.
        filter(ea => ea.behaviorId !== timestampedBehavior.behaviorId ).
        concat([timestampedBehavior]);
      return group.clone({ behaviorVersions: updatedVersions });
    }

    getPersistentId() {
      return this.behaviorId;
    }

    isBehaviorVersion() {
      return true;
    }

    copyWithNewTimestamp() {
      return this.clone({ createdAt: Date.now() });
    }

    isDataType() {
      return this.config.isDataType;
    }

    getDescription() {
      return this.description || "";
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

    includesText(queryString) {
      var lowercase = queryString.toLowerCase().trim();
      return this.getName().toLowerCase().includes(lowercase) ||
          this.getDescription().toLowerCase().includes(lowercase) ||
          this.getTriggers().some((trigger) => (
            trigger.getText().toLowerCase().includes(lowercase)
          ));
    }

    // Used by JSON.stringify for submitting data to the server
    toJSON() {
      return this.clone({
        createdAt: null,
        editorScrollPosition: null
      });
    }

    isIdenticalToVersion(behaviorVersion) {
      return DeepEqual.isEqual(this.forEqualityComparison(), behaviorVersion.forEqualityComparison());
    }

    timestampForAlphabeticalSort() {
      const timestampString = Number(new Date(this.createdAt)).toString();
      const pad = new Array(16).join("0");
      return pad.substring(0, pad.length - timestampString.length) + timestampString;
    }

    get sortKey() {
      if (this.isNewBehavior) {
        return "Z" + this.timestampForAlphabeticalSort();
      } else {
        return "A" + (this.name || this.getFirstTriggerText() || this.timestampForAlphabeticalSort());
      }
    }

    toParamType() {
      return {
        id: this.id,
        exportId: this.exportId,
        name: this.name || "Unnamed data type"
      };
    }

    clone(props) {
      return new BehaviorVersion(Object.assign({}, this, props));
    }

    static fromJson(props) {
      const materializedProps = Object.assign({}, props, {
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
