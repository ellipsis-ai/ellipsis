define(function(require) {
  var DeepEqual = require('../lib/deep_equal');

  class Editable {

    constructor(props) {
      const initialProps = Object.assign({
        name: '',
        description: '',
        functionBody: '',
        editorScrollPosition: 0
      }, props);

      Object.defineProperties(this, {
        id: { value: initialProps.id, enumerable: true },
        groupId: { value: initialProps.groupId, enumerable: true },
        teamId: { value: initialProps.teamId, enumerable: true },
        isNew: { value: initialProps.isNew, enumerable: true },
        name: { value: initialProps.name, enumerable: true },
        description: { value: initialProps.description, enumerable: true },
        functionBody: { value: initialProps.functionBody, enumerable: true },
        exportId: { value: initialProps.exportId, enumerable: true },
        editorScrollPosition: { value: initialProps.editorScrollPosition, enumerable: true }
      });
    }

    isBehaviorVersion() {
      return false;
    }

    isLibraryVersion() {
      return false;
    }

    getName() {
      return this.name || "";
    }

    getDescription() {
      return this.description || "";
    }

    includesText(queryString) {
      var lowercase = queryString.toLowerCase().trim();
      return this.getName().toLowerCase().includes(lowercase) ||
        this.getDescription().toLowerCase().includes(lowercase);
    }

    // Used by JSON.stringify for submitting data to the server
    toJSON() {
      return this.clone({
        editorScrollPosition: null
      });
    }

    forEqualityComparison() {
      return this.toJSON();
    }

    isIdenticalToVersion(version) {
      return DeepEqual.isEqual(this.forEqualityComparison(), version.forEqualityComparison());
    }

    static forEqualityComparison(version) {
      return version.forEqualityComparison();
    }
  }

  return Editable;
});
