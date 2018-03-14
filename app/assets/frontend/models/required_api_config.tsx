export interface RequiredApiConfigJson {
  id?: Option<string>,
  exportId?: Option<string>,
  apiId: string,
  nameInCode: string
}

class RequiredApiConfig implements RequiredApiConfigJson {
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
  }

export default RequiredApiConfig;

