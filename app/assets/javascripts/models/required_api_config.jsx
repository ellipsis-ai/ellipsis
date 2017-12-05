// @flow
define(function(require) {
  const diffs = require('./diffs');

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

    maybeNameInCodeDiffFor(other: RequiredApiConfig): ?diffs.CategoricalPropertyDiff {
      return diffs.TextPropertyDiff.maybeFor("Name used in code", this.nameInCode, other.nameInCode);
    }

    canHaveConfig(): boolean {
      return false;
    }
  }

  return RequiredApiConfig;
});
