define(function() {

  class ParamType {
    constructor(props) {
      var initialProps = Object.assign({
        name: "",
        needsConfig: false,
        isBuiltIn: false
      }, props);

      Object.defineProperties(this, {
        id: { value: initialProps.id, enumerable: true },
        exportId: { value: initialProps.exportId, enumerable: true },
        isBuiltIn: { value: initialProps.isBuiltIn, enumerable: true },
        name: { value: initialProps.name, enumerable: true },
        needsConfig: { value: initialProps.needsConfig, enumerable: true }
      });
    }

    getBehaviorVersionId() {
      return this.isBuiltIn ? null : this.id;
    }

    clone(newProps) {
      return new ParamType(Object.assign({}, this, newProps));
    }

    static fromJson(props) {
      return new ParamType(props);
    }
  }

  return ParamType;
});
