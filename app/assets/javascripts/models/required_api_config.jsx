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
      return `required ${this.apiId} configuration "${this.nameInCode}"`;
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
