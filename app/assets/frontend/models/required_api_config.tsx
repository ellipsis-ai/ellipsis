import ApiConfigRef from "./api_config_ref";

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

    abstract onAddConfigFor(editor: RequiredApiConfigEditor): (required: RequiredApiConfig, callback?: () => void) => void
    abstract onAddNewConfigFor(editor: RequiredApiConfigEditor): (required?: RequiredApiConfig, callback?: () => void) => void
    abstract onRemoveConfigFor(editor: RequiredApiConfigEditor): (required: RequiredApiConfig, callback?: () => void) => void
    abstract onUpdateConfigFor(editor: RequiredApiConfigEditor): (required: RequiredApiConfig, callback?: () => void) => void
    abstract getApiLogoUrl(editor: RequiredApiConfigEditor): string
    abstract getApiName(editor: RequiredApiConfigEditor): string
    abstract getAllConfigsFrom(editor: RequiredApiConfigEditor): Array<ApiConfigRef>

    abstract clone(props: Partial<RequiredApiConfig>): RequiredApiConfig
}

export default RequiredApiConfig;

export interface RequiredApiConfigEditor {}
