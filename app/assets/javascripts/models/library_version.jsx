define(function(require) {
  const Editable = require('./editable');

  class LibraryVersion extends Editable {
    constructor(props) {
      super();

      Object.defineProperties(this, {
        libraryId: { value: props.libraryId, enumerable: true }
      });
    }

    clone(props) {
      return new LibraryVersion(Object.assign({}, this, props));
    }

  }

  return LibraryVersion;
});
