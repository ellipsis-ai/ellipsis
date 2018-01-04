// @flow

define(function() {

  class RequiredApiConfig {
    id: string;
    requiredId: string;
    apiId: string;
    nameInCode: string;

    constructor(id: string, requiredId: string, apiId: string, nameInCode: string) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        requiredId: { value: requiredId, enumerable: true },
        apiId: { value: apiId, enumerable: true },
        nameInCode: { value: nameInCode, enumerable: true }
      });
    }

    diffLabel(): string {
      return `required ${this.apiId} configuration "${this.nameInCode}"`;
    }

    getIdForDiff(): string {
      return this.requiredId;
    }

    canHaveConfig(): boolean {
      return false;
    }
  }

  return RequiredApiConfig;
});
