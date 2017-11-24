// @flow
define(function() {
  class RequiredApiConfig {
    id: string;
    apiId: string;
    nameInCode: string;

    constructor(id: string, apiId: string, nameInCode: string) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        apiId: { value: apiId, enumerable: true },
        nameInCode: { value: nameInCode, enumerable: true }
      });
    }

    canHaveConfig(): boolean {
      return false;
    }
  }

  return RequiredApiConfig;
});
