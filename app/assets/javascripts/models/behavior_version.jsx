// @flow
define(function(require) {
  const
    BehaviorConfig = require('./behavior_config'),
    DataTypeConfig = require('./data_type_config'),
    DeepEqual = require('../lib/deep_equal'),
    Editable = require('./editable'),
    ParamType = require('./param_type'),
    ResponseTemplate = require('./response_template'),
    Trigger = require('./trigger');

  class BehaviorVersion extends Editable {
    id: ?string;
    behaviorId: string;
    responseTemplate: ?string;
    functionBody: string;
    inputIds: Array<string>;
    triggers: Array<Trigger>;
    config: BehaviorConfig;
    knownEnvVarsUsed: Array<string>;
    createdAt: ?number;
    shouldRevealCodeEditor: boolean;
    isNew: ?boolean;

    constructor(
      id: ?string,
      behaviorId: string,
      groupId: string,
      teamId: string,
      name: ?string,
      description: ?string,
      responseTemplate: ?string,
      functionBody: string,
      inputIds: Array<string>,
      triggers: Array<Trigger>,
      config: BehaviorConfig,
      exportId: ?string,
      knownEnvVarsUsed: Array<string>,
      createdAt: ?number,
      shouldRevealCodeEditor: ?boolean,
      isNew: ?boolean,
      editorScrollPosition: number
    ) {
      super(
        id,
        groupId,
        teamId,
        isNew,
        name,
        description,
        functionBody,
        exportId,
        editorScrollPosition
      );

      const revealCodeEditor: boolean = shouldRevealCodeEditor || !!functionBody && functionBody.length > 0;

      Object.defineProperties(this, {
        behaviorId: { value: behaviorId, enumerable: true },
        responseTemplate: { value: responseTemplate, enumerable: true },
        inputIds: { value: inputIds || [], enumerable: true },
        triggers: { value: triggers || [], enumerable: true },
        config: { value: config, enumerable: true },
        knownEnvVarsUsed: { value: knownEnvVarsUsed || [], enumerable: true },
        createdAt: { value: createdAt, enumerable: true },
        shouldRevealCodeEditor: { value: revealCodeEditor, enumerable: true }
      });
    }

    namePlaceholderText(): string {
      return this.isDataType() ? "Data type name (required)" : "Action name (optional)";
    }

    cloneActionText(): string {
      return this.isDataType() ? "Clone data type…" : "Clone action…";
    }

    deleteActionText(): string {
      return this.isDataType() ? "Delete data type…" : "Delete action…";
    }

    confirmDeleteText(): string {
      const behaviorType = this.isDataType() ? "data type" : "action";
      return `Are you sure you want to delete this ${behaviorType}?`;
    }

    cancelNewText(): string {
      return this.isDataType() ? "Cancel new data type" : "Cancel new action";
    }

    buildUpdatedGroupFor(group, props) {
      const timestampedBehavior = this.clone(props).copyWithNewTimestamp();
      const updatedVersions = group.behaviorVersions.
        filter(ea => ea.behaviorId !== timestampedBehavior.behaviorId ).
        concat([timestampedBehavior]);
      return group.clone({ behaviorVersions: updatedVersions });
    }

    getPersistentId(): string {
      return this.behaviorId;
    }

    isBehaviorVersion(): boolean {
      return true;
    }

    copyWithNewTimestamp(): BehaviorVersion {
      return this.clone({ createdAt: Date.now() });
    }

    isDataType(): boolean {
      return this.config.isDataType;
    }

    usesCode(): boolean {
      return !this.isDataType() || this.getDataTypeConfig().usesCode;
    }

    getNewEditorTitle(): string {
      return this.isDataType() ? "New data type" : "New action";
    }

    getExistingEditorTitle(): string {
      return this.isDataType() ? "Edit data type" : "Edit action";
    }

    getDataTypeConfig(): DataTypeConfig {
      return this.config.getDataTypeConfig();
    }

    getDataTypeFields() {
      return this.getDataTypeConfig() ? this.getDataTypeConfig().getFields() : [];
    }

    getWritableDataTypeFields() {
      return this.getDataTypeConfig() ? this.getDataTypeConfig().getWritableFields() : [];
    }

    getGraphQLListQueryName(): string {
      const name = this.getName();
      if (name) {
        return name.replace(/^./, (firstLetter) => firstLetter.toLowerCase()) + "List";
      } else {
        return "";
      }
    }

    buildGraphQLListQuery(): string {
      const fieldNames = this.getDataTypeFields().map((ea) => ea.name);
      const queryName = this.getGraphQLListQueryName();
      if (fieldNames.length === 0) {
        throw new Error("Unable to create a GraphQL query: no fields found");
      }
      if (fieldNames.some((ea) => !ea)) {
        throw new Error("Unable to create a GraphQL query: unnamed fields");
      }
      if (!queryName) {
        throw new Error("Unable to create a GraphQL query: data type has no name");
      }
      return `{ ${queryName}(filter: {}) { ${fieldNames.join(" ")} } }`;
    }

    requiresFields(): boolean {
      return this.getDataTypeConfig() ? this.getDataTypeConfig().requiresFields() : false;
    }

    getTriggers(): Array<Trigger> {
      return this.triggers;
    }

    findFirstTriggerIndexForDisplay(): number {
      var firstTriggerIndex = this.getTriggers().findIndex(function(trigger) {
        return !!trigger.text && !trigger.isRegex;
      });
      if (firstTriggerIndex === -1) {
        firstTriggerIndex = 0;
      }
      return firstTriggerIndex;
    }

    getFirstTriggerText(): string {
      var trigger = this.getTriggers()[this.findFirstTriggerIndexForDisplay()];
      if (trigger) {
        return trigger.text;
      } else {
        return "";
      }
    }

    getFunctionBody(): string {
      return this.functionBody || "";
    }

    includesText(queryString): boolean {
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

    isIdenticalToVersion(behaviorVersion): boolean {
      return DeepEqual.isEqual(this.forEqualityComparison(), behaviorVersion.forEqualityComparison());
    }

    timestampForAlphabeticalSort(): string {
      const timestampString = this.createdAt ? Number(new Date(this.createdAt)).toString() : "";
      const pad = new Array(16).join("0");
      return pad.substring(0, pad.length - timestampString.length) + timestampString;
    }

    sortKey(): string {
      if (this.isNew) {
        return "Z" + this.timestampForAlphabeticalSort();
      } else {
        return "A" + (this.name || this.getFirstTriggerText() || this.timestampForAlphabeticalSort());
      }
    }

    toParamType(): ParamType {
      return ParamType.fromProps({
        id: this.id,
        exportId: this.exportId,
        name: this.name || "Unnamed data type"
      });
    }

    clone(props): BehaviorVersion {
      return BehaviorVersion.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props): BehaviorVersion {
      return new BehaviorVersion(
        props.id,
        props.behaviorId,
        props.groupId,
        props.teamId,
        props.name,
        props.description,
        props.responseTemplate,
        props.functionBody,
        props.inputIds,
        props.triggers,
        props.config,
        props.exportId,
        props.knownEnvVarsUsed,
        props.createdAt,
        props.shouldRevealCodeEditor,
        props.isNew,
        props.editorScrollPosition
      );
    }

    static fromJson(props): BehaviorVersion {
      const materializedProps = Object.assign({}, props, {
        responseTemplate: ResponseTemplate.fromString(props.responseTemplate || '')
      });
      if (props.config) {
        materializedProps.config = BehaviorConfig.fromJson(props.config);
      }
      if (props.triggers) {
        materializedProps.triggers = Trigger.triggersFromJson(props.triggers);
      }
      return BehaviorVersion.fromProps(materializedProps);
    }

  }

  return BehaviorVersion;
});
