import {Diffable, DiffableProp} from "./diffs";
import BehaviorGroup from "./behavior_group";
import BehaviorConfig, {BehaviorConfigJson} from './behavior_config';
import DataTypeConfig from './data_type_config';
import Editable, {EditableInterface, EditableJson} from './editable';
import Input from './input';
import ParamType from './param_type';
import ResponseTemplate, {ResponseTemplateJson} from './response_template';
import Trigger, {TriggerJson, TriggerType} from './trigger';
import DataTypeField from "./data_type_field";
import {Timestamp} from "../lib/formatter";
import {NODE_JS_VERSION} from "../lib/constants";

type DefaultActionProps = {
  name?: Option<string>,
  triggers: Array<Trigger>,
  functionBody: string,
  responseTemplate: ResponseTemplate
}

export interface BehaviorVersionJson extends EditableJson {
  behaviorId: string;
  responseTemplate?: Option<ResponseTemplateJson>;
  inputIds: Array<string>;
  triggers: Array<TriggerJson>;
  config: BehaviorConfigJson;
}

export interface BehaviorVersionInterface extends EditableInterface, BehaviorVersionJson {
  responseTemplate?: Option<ResponseTemplate>;
  triggers: Array<Trigger>;
  config: BehaviorConfig;
}

class BehaviorVersion extends Editable implements Diffable, BehaviorVersionInterface, EditableInterface {
  constructor(
    readonly id: Option<string>,
    readonly behaviorId: string,
    readonly groupId: Option<string>,
    readonly teamId: Option<string>,
    readonly name: Option<string>,
    readonly description: Option<string>,
    readonly responseTemplate: Option<ResponseTemplate>,
    readonly functionBody: string,
    readonly inputIds: Array<string>,
    readonly triggers: Array<Trigger>,
    readonly config: BehaviorConfig,
    readonly exportId: Option<string>,
    readonly createdAt: Option<Timestamp>,
    readonly isNew: Option<boolean>
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
        createdAt
      );

      Object.defineProperties(this, {
        behaviorId: { value: behaviorId, enumerable: true },
        responseTemplate: { value: responseTemplate, enumerable: true },
        inputIds: { value: inputIds || [], enumerable: true },
        triggers: { value: triggers || [], enumerable: true },
        config: { value: config, enumerable: true }
      });
  }

    responseTemplateText(): string {
      return this.responseTemplate ? this.responseTemplate.text : "";
    }

    dataTypeUsesCode(): boolean {
      return Boolean(this.config.dataTypeConfig && this.config.dataTypeConfig.usesCode);
    }

    inputsFor(group?: BehaviorGroup): Array<Input> {
      if (group) {
        const allInputs = group.getInputs();
        const matchingInputs: Array<Input> = [];
        this.inputIds.forEach((eaId) => {
          const input = allInputs.find(ea => ea.inputId === eaId);
          if (input) {
            matchingInputs.push(input);
          }
        });
        return matchingInputs;
      } else {
        return [];
      }
    }

    diffProps(parent?: BehaviorGroup): Array<DiffableProp> {
      return [{
        name: "Name",
        value: this.getName()
      }, {
        name: "Description",
        value: this.getDescription()
      }, {
        name: "Response template",
        value: this.responseTemplateText(),
        isCode: true
      }, {
        name: "Code",
        value: this.getFunctionBody(),
        isCode: true
      }, {
        name: "Response type",
        value: this.config.responseTypeId,
        isCategorical: true
      }, {
        name: "Cache results",
        value: Boolean(this.config.canBeMemoized)
      }, {
        name: "Code-backed data type",
        value: this.dataTypeUsesCode()
      }, {
        name: "Triggers",
        value: this.getTriggers()
      }, {
        name: "Inputs",
        value: this.inputsFor(parent),
        isOrderable: true
      }];
    }

    namePlaceholderText(): string {
      const label = this.kindLabel();
      return `${label[0].toUpperCase()}${label.slice(1)} name`;
    }

    cloneActionText(): string {
      return `Clone ${this.kindLabel()}…`;
    }

    deleteActionText(): string {
      return `Delete ${this.kindLabel()}…`;
    }

    confirmDeleteText(): string {
      return `Are you sure you want to delete this ${this.kindLabel()}?`;
    }

    cancelNewText(): string {
      return `Cancel new ${this.kindLabel()}`;
    }

    buildUpdatedGroupFor(group: BehaviorGroup, props: Partial<BehaviorVersionInterface>): BehaviorGroup {
      const timestampedBehavior = this.clone(props).copyWithNewTimestamp();
      const updatedVersions = group.behaviorVersions.
        filter(ea => ea.behaviorId !== timestampedBehavior.behaviorId ).
        concat([timestampedBehavior]);
      return group.clone({ behaviorVersions: updatedVersions });
    }

    getPersistentId(): string {
      return this.behaviorId;
    }

    getIdForDiff(): string {
      return this.config.exportId || "unknown";
    }

    isBehaviorVersion(): this is BehaviorVersion {
      return true;
    }

    copyWithNewTimestamp(): BehaviorVersion {
      return this.clone({ createdAt: Date.now() });
    }

    isDataType(): boolean {
      return this.config.isDataType;
    }

    isTest(): boolean {
      return this.config.isTest;
    }

    usesCode(): boolean {
      const config = this.getDataTypeConfig();
      return !this.isDataType() || Boolean(config && config.usesCode);
    }

    hasDefaultDataTypeSettings(): boolean {
      return this.isDataType() && this.usesCode() && !this.functionBody;
    }

    getBehaviorVersionTypeName(): string {
      if (this.isDataType()) {
        return "data type";
      } else if (this.isTest()) {
        return "test";
      } else {
        return "action";
      }
    }

    diffLabel(): string {
      const itemLabel = this.itemLabel();
      const kindLabel = this.kindLabel();
      return itemLabel ? `${kindLabel} “${itemLabel}”` : `unnamed ${kindLabel}`;
    }

    itemLabel(): Option<string> {
      return this.getName();
    }

    kindLabel(): string {
      return this.getBehaviorVersionTypeName();
    }

    getNewEditorTitle(): string {
      return `New ${this.getBehaviorVersionTypeName()}`;
    }

    getExistingEditorTitle(): string {
      return `Edit ${this.getBehaviorVersionTypeName()}`;
    }

    getDataTypeConfig(): Option<DataTypeConfig> {
      return this.config.getDataTypeConfig();
    }

    getDataTypeFields(): Array<DataTypeField> {
      const config = this.getDataTypeConfig();
      return config ? config.getFields() : [];
    }

    getWritableDataTypeFields(): Array<DataTypeField> {
      const config = this.getDataTypeConfig();
      return config ? config.getWritableFields() : [];
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
      const config = this.getDataTypeConfig();
      return config ? config.requiresFields() : false;
    }

    getTriggers(): Array<Trigger> {
      return this.triggers;
    }

    getRegularMessageTriggers(): Array<Trigger> {
      return this.triggers.filter((ea) => ea.isMessageSentTrigger() && !ea.isRegex);
    }

    findFirstTriggerIndexForDisplay(): number {
      var firstTriggerIndex = this.getTriggers().findIndex(function(trigger) {
        return !!trigger.text && !trigger.isReactionAddedTrigger() && !trigger.isRegex;
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

    includesText(queryString: string): boolean {
      var lowercase = queryString.toLowerCase().trim();
      return super.includesText(queryString) ||
          this.getTriggers().some((trigger) => (
            trigger.getText().toLowerCase().includes(lowercase)
          ));
    }

    // Used by JSON.stringify for submitting data to the server
    toJSON(): BehaviorVersion {
      return (super.toJSON() as BehaviorVersion).clone({
        createdAt: null
      });
    }

    forEqualityComparison(): BehaviorVersion {
      return this.toJSON();
    }

    sortKeyForExisting(): Option<string> {
      return this.name || this.getFirstTriggerText();
    }

    toParamType(): ParamType {
      return ParamType.fromProps({
        id: this.id || "unknown",
        exportId: this.exportId || "unknown",
        name: this.name || "Unnamed data type",
        typescriptType: ParamType.typescriptTypeForDataTypes()
      });
    }

    isEmpty(): boolean {
      const name = this.getName();
      const triggers = this.getTriggers();
      const inputs = this.inputIds;
      const code = this.getFunctionBody();
      const template = this.responseTemplateText();

      return (
        (!name) &&
        (triggers.length === 0 || triggers.every((trigger) => !trigger.getText())) &&
        (inputs.length === 0) &&
        (!code) &&
        (!template)
      );
    }

    clone(props: Partial<BehaviorVersionInterface>): BehaviorVersion {
      return BehaviorVersion.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props: BehaviorVersionInterface): BehaviorVersion {
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
        props.createdAt,
        props.isNew
      );
    }

    static fromJson(props: BehaviorVersionJson): BehaviorVersion {
      const materializedProps = Object.assign({}, props, {
        responseTemplate: typeof props.responseTemplate === 'string' ?
          ResponseTemplate.fromString(props.responseTemplate || '') :
          new ResponseTemplate(props.responseTemplate && props.responseTemplate.text),
        config: BehaviorConfig.fromJson(props.config),
        triggers: Trigger.triggersFromJson(props.triggers)
      });
      return BehaviorVersion.fromProps(materializedProps);
    }

    static defaultActionProps(optionalName?: string): DefaultActionProps {
      const functionBody = (
`// Write a Node.js (${NODE_JS_VERSION}) function that calls ellipsis.success() with a result.
// You can require any NPM package.
const name = ellipsis.event.user.fullName || "friend";
ellipsis.success(\`Hello, \${name}.\`);
`);
      const template = ResponseTemplate.fromString("{successResult}");
      const triggers = [new Trigger(TriggerType.MessageSent, `run ${optionalName || "action"}`, false, true)];
      const props: DefaultActionProps = {
        triggers: triggers,
        functionBody: functionBody,
        responseTemplate: template
      };
      if (optionalName) {
        props.name = optionalName;
      }
      return props;
    }

    static defaultDataTypeCode(): string {
      return (
`// Write a Node.js (${NODE_JS_VERSION}) function that calls ellipsis.success() with an array of items.
//
// Each item requires a "label" and "id" property.
//
// Example:
//
// ellipsis.success([
//   { id: "1", label: "One" },
//   { id: "2", label: "Two" }
// ]);
`);
    }

    icon(): string {
      if (this.isDataType()) {
        return BehaviorVersion.dataTypeIcon();
      } else if (this.isTest()) {
        return BehaviorVersion.testIcon();
      } else {
        return BehaviorVersion.actionIcon();
      }
    }

    static actionIcon(): string {
      return "🎬";
    }

    static dataTypeIcon(): string {
      return "📁";
    }

    static testIcon(): string {
      return "📐";
    }

}

export default BehaviorVersion;
