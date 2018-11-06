import * as React from 'react';
import NodeModuleVersion from '../models/node_module_version';
import autobind from '../lib/autobind';

interface Props {
  nodeModuleVersions: Array<NodeModuleVersion>,
  updatingNodeModules: Boolean
}

class NodeModuleList extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  getTitleFor(version: NodeModuleVersion): string {
    if (this.props.updatingNodeModules) {
      return `${version.from} (updating)`;
    } else {
      return `${version.from} v${version.version}`;
    }
  }

  renderVersion(version: NodeModuleVersion) {
    return (
      <div
        key={`nodeModuleVersion-${version.from}-${version.version}`}
        className={`pbxs`}
      >
        <div className="phxl mobile-phl type-monospace display-ellipsis" title={this.getTitleFor(version)}>
          {this.props.updatingNodeModules ? (
            <span>
              <span>{version.from}</span>
              <span className="pulse type-disabled">@...</span>
            </span>
          ) : (
            <a href={version.getUrl()} target="_blank">
              <span>{version.from}</span>
              <span className="type-disabled">@</span>
              <span className="type-weak">{version.version}</span>
            </a>
          )}
        </div>
      </div>
    );
  }

  render() {
    return (
      <div>{this.props.nodeModuleVersions.map(this.renderVersion)}</div>
    );
  }
}

export default NodeModuleList;
