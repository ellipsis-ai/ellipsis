define(function(require) {
  class NodeModuleVersion {
    constructor(props) {
      Object.defineProperties(this, {
        from: { value: props.from, enumerable: true },
        version: { value: props.version, enumerable: true }
      });
    }

    clone(props) {
      return new NodeModuleVersion(Object.assign({}, this, props));
    }

  }

  return NodeModuleVersion;
});
