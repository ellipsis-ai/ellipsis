define(function(require) {
  var
    DataTypeConfig = require('./data_type_config'),
    DeepEqual = require('../lib/deep_equal'),
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
        createdAt: Date.now(),
        dataTypeConfig: null
      }, props);

      Object.defineProperties(this, {
        behaviorId: { value: initialProps.behaviorId, enumerable: true },
        responseTemplate: { value: initialProps.responseTemplate, enumerable: true },
        inputIds: { value: initialProps.inputIds, enumerable: true },
        triggers: { value: initialProps.triggers, enumerable: true },
        config: { value: initialProps.config, enumerable: true },
        knownEnvVarsUsed: { value: initialProps.knownEnvVarsUsed, enumerable: true },
        createdAt: { value: initialProps.createdAt, enumerable: true },
        shouldRevealCodeEditor: { value: initialProps.shouldRevealCodeEditor, enumerable: true },
        dataTypeConfig: { value: initialProps.dataTypeConfig, enumerable: true }
      });
    }

    namePlaceholderText() {
      return this.isDataType() ? "Data type name (required)" : "Action name (optional)";
    }

    cloneActionText() {
      return this.isDataType() ? "Clone data type…" : "Clone action…";
    }

    deleteActionText() {
      return this.isDataType() ? "Delete data type…" : "Delete action…";
    }

    confirmDeleteText() {
      const behaviorType = this.isDataType() ? "data type" : "action";
      return `Are you sure you want to delete this ${behaviorType}?`;
    }

    cancelNewText() {
      return this.isDataType() ? "Cancel new data type" : "Cancel new action";
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

    getNewEditorTitle() {
      return this.isDataType() ? "New data type" : "New action";
    }

    getExistingEditorTitle() {
      return this.isDataType() ? "Edit data type" : "Edit action";
    }

    getDataTypeConfig() {
      return this.dataTypeConfig;
    }

    getDataTypeFields() {
      return this.getDataTypeConfig() ? this.getDataTypeConfig().getFields() : [];
    }

    requiresFields() {
      return this.getDataTypeConfig() ? this.getDataTypeConfig().requiresFields() : false;
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

    getFunctionBody() {
      return this.functionBody || "";
    }

    includesText(queryString) {
      var lowercase = queryString.toLowerCase().trim();
      return super.includesText(queryString) ||
          this.getTriggers().some((trigger) => (
            trigger.getText().toLowerCase().includes(lowercase)
          ));
    }

    // Used by JSON.stringify for submitting data to the server
    toJSON() {
      return super.toJSON().clone({
        createdAt: null
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
      if (this.isNew) {
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
      if (props.dataTypeConfig) {
        materializedProps.dataTypeConfig = DataTypeConfig.fromJson(props.dataTypeConfig);
      }
      if (props.triggers) {
        materializedProps.triggers = Trigger.triggersFromJson(props.triggers);
      }
      return new BehaviorVersion(materializedProps);
    }

  }

  return BehaviorVersion;
});
