export interface NodeModuleVersionJson {
  from: string,
  version: string
}

interface NodeModuleVersionInterface extends NodeModuleVersionJson {}

class NodeModuleVersion implements NodeModuleVersionInterface {
  constructor(
    readonly from: string,
    readonly version: string
  ) {
    Object.defineProperties(this, {
      from: {value: from, enumerable: true},
      version: {value: version, enumerable: true}
    });
  }

  clone(props: Partial<NodeModuleVersionInterface>): NodeModuleVersion {
    return NodeModuleVersion.fromProps(Object.assign({}, this, props));
  }

  static fromProps(props: NodeModuleVersionInterface): NodeModuleVersion {
    return new NodeModuleVersion(props.from, props.version);
  }

  static allFromJson(jsonArray: Array<NodeModuleVersionJson>) {
    return jsonArray.map((props) => NodeModuleVersion.fromProps(props));
  }

}

export default NodeModuleVersion;

