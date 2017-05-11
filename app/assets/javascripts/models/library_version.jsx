define(function(require) {
  var DeepEqual = require('../lib/deep_equal');

  class LibraryVersion {
    constructor(props) {
      var initialProps = Object.assign({
        code: '',
        editorScrollPosition: 0,
      }, props);

      Object.defineProperties(this, {
        id: { value: initialProps.id, enumerable: true },
        libraryId: { value: initialProps.libraryId, enumerable: true },
        groupId: { value: initialProps.groupId, enumerable: true },
        teamId: { value: initialProps.teamId, enumerable: true },
        name: { value: initialProps.name, enumerable: true },
        code: { value: initialProps.functionBody, enumerable: true },
        editorScrollPosition: { value: initialProps.editorScrollPosition, enumerable: true }
      });
    }

    getName() {
      return this.name || "";
    }

    includesText(queryString) {
      var lowercase = queryString.toLowerCase().trim();
      return this.getName().toLowerCase().includes(lowercase);
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

    isIdenticalToVersion(libraryVersion) {
      return DeepEqual.isEqual(this.forEqualityComparison(), libraryVersion.forEqualityComparison());
    }

    clone(props) {
      return new LibraryVersion(Object.assign({}, this, props));
    }

    static forEqualityComparison(version) {
      return version.forEqualityComparison();
    }
  }

  return LibraryVersion;
});
