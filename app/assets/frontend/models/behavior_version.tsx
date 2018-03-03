import {Diffable, DiffableProp} from "./diffs";
import {HasInputs} from "./input";
import BehaviorGroup from "./behavior_group";

import BehaviorConfig from './behavior_config';
import DataTypeConfig from './data_type_config';
import Editable from './editable';
import Input from './input';
import ParamType from './param_type';
import ResponseTemplate from './response_template';
import Trigger from './trigger';

type DefaultActionProps = {
  name?: string,
  triggers: Array<Trigger>,
  functionBody: string,
  responseTemplate: ResponseTemplate
}

class BehaviorVersion extends Editable implements Diffable {
    id?: string | null;
    behaviorId: string;
    responseTemplate?: ResponseTemplate | null;
    functionBody: string;
    inputIds: Array<string>;
    triggers: Array<Trigger>;
    config: BehaviorConfig;
    knownEnvVarsUsed: Array<string>;
    isNew?: boolean | null;

    constructor(
      id: string | null,
      behaviorId: string,
      groupId: string,
      teamId: string,
      name: string | null,
      description: string | null,
      responseTemplate: ResponseTemplate | null,
      functionBody: string,
      inputIds: Array<string>,
      triggers: Array<Trigger>,
      config: BehaviorConfig,
      exportId: string | null,
      knownEnvVarsUsed: Array<string>,
      createdAt: number | null,
      isNew: boolean | null,
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
        editorScrollPosition,
        createdAt
      );

      Object.defineProperties(this, {
        behaviorId: { value: behaviorId, enumerable: true },
        responseTemplate: { value: responseTemplate, enumerable: true },
        inputIds: { value: inputIds || [], enumerable: true },
        triggers: { value: triggers || [], enumerable: true },
        config: { value: config, enumerable: true },
        knownEnvVarsUsed: { value: knownEnvVarsUsed || [], enumerable: true }
      });
    }

    responseTemplateText(): string {
      return this.responseTemplate ? this.responseTemplate.text : "";
    }

    dataTypeUsesCode(): boolean {
      return this.config.dataTypeConfig && this.config.dataTypeConfig.usesCode;
    }

    inputsFor(group?: HasInputs): Array<Input> {
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

    diffProps(parent?: HasInputs): Array<DiffableProp> {
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
        name: "Always responds privately",
        value: this.config.forcePrivateResponse
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
      return this.isDataType() ? "Data type name" : "Action name";
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

    buildUpdatedGroupFor(group: BehaviorGroup, props: {}): BehaviorGroup {
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
      return this.config.exportId;
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

    getBehaviorVersionTypeName(): string {
      return this.isDataType() ? "data type" : "action";
    }

    diffLabel(): string {
      const itemLabel = this.itemLabel();
      const kindLabel = this.kindLabel();
      return itemLabel ? `${kindLabel} “${itemLabel}”` : `unnamed ${kindLabel}`;
    }

    itemLabel(): string | null {
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

    includesText(queryString: string): boolean {
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

    sortKeyForExisting(): string | null {
      return this.name || this.getFirstTriggerText();
    }

    toParamType(): ParamType {
      return ParamType.fromProps({
        id: this.id || "unknown",
        exportId: this.exportId || "unknown",
        name: this.name || "Unnamed data type"
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

    clone(props: {}): BehaviorVersion {
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

    static defaultActionProps(optionalName): DefaultActionProps {
      const functionBody = (
`// Write a Node.js (6.10.2) function that calls ellipsis.success() with a result.
// You can require any NPM package.
const name = ellipsis.userInfo.fullName || "friend";
ellipsis.success(name);
`);
      const template = ResponseTemplate.fromString("Hello, {successResult}");
      const triggers = [new Trigger(`run ${optionalName || "action"}`, false, true)];
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

    static defaultDataTypeCode(usesSearch): string {
      return (
`// Write a Node.js (6.10.2) function that calls ellipsis.success() with an array of items.
// ${usesSearch ? "Use searchQuery to filter on the user’s input." : ""}
// Each item should have a "label" and "id" property.
//
// Example:
//
// ellipsis.success([
//   { id: "1", label: "One" },
//   { id: "2", label: "Two" }
// ]);
`);
    }

}

export default BehaviorVersion;
