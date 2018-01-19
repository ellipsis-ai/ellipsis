// @flow

define(function() {

  class RequiredApiConfig {
    id: string;
    exportId: string;
    apiId: string;
    nameInCode: string;

    constructor(id: string, exportId: string, apiId: string, nameInCode: string) {
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

    itemLabel(): ?string {
      return this.nameInCode;
    }

    kindLabel(): string {
      return `required API configuration`;
    }

    getIdForDiff(): string {
      return this.exportId;
    }

    canHaveConfig(): boolean {
      return false;
    }
  }

  return RequiredApiConfig;
});
