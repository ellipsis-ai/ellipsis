// @flow
class NodeModuleVersion {
    from: string;
    version: string;

    constructor(from: string, version: string) {
      Object.defineProperties(this, {
        from: { value: from, enumerable: true },
        version: { value: version, enumerable: true }
      });
    }

    clone(props: {}): NodeModuleVersion {
      return NodeModuleVersion.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props): NodeModuleVersion {
      return new NodeModuleVersion(props.from, props.version);
    }

    static allFromJson(jsonArray) {
      return jsonArray.map((props) => NodeModuleVersion.fromProps(props));
    }

}

export default NodeModuleVersion;

