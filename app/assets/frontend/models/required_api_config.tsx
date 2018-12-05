import ApiConfigRef from "./api_config_ref";
import BehaviorEditor from "../behavior_editor";
import {ApiConfigEditor} from "../behavior_editor/api_config_panel";

export interface RequiredApiConfigJson {
  id?: Option<string>,
  exportId?: Option<string>,
  apiId: string,
  nameInCode: string
}

abstract class RequiredApiConfig implements RequiredApiConfigJson {
  readonly config?: Option<ApiConfigRef>;

  constructor(
    readonly id: Option<string>,
    readonly exportId: Option<string>,
    readonly apiId: string,
    readonly nameInCode: string
  ) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        exportId: { value: exportId, enumerable: true },
        apiId: { value: apiId, enumerable: true },
        nameInCode: { value: nameInCode, enumerable: true }
      });
    }

    diffLabel(): string {
      const itemLabel = this.itemLabel();
      const kindLabel = this.kindLabel();
      return itemLabel ? `${kindLabel} "${itemLabel}"`: `unnamed ${kindLabel}`;
    }

    itemLabel(): Option<string> {
      return this.nameInCode;
    }

    kindLabel(): string {
      return `required API configuration`;
    }

    getIdForDiff(): string {
      return this.exportId || "unknown";
    }

    canHaveConfig(): boolean {
      return false;
    }

    abstract codePath(): string

    abstract codePathPrefix(): string

    abstract isConfigured(): boolean

    abstract configName(): Option<string>

    abstract clone(props: Partial<RequiredApiConfig>): this

    abstract editorFor(editor: BehaviorEditor): ApiConfigEditor<any>
}

export default RequiredApiConfig;

export interface RequiredApiConfigEditor {}
