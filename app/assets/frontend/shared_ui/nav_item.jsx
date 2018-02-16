// @flow
import * as React from 'react';
import SVGDivider from '../svg/divider';

type Props = {
  children: React.Node
};

class NavItem extends React.PureComponent<Props> {
  render(): React.Node {
    return (
      <div>
        <div className="column prn display-inline-block height-button-l align-t color-black-translucent">
          <SVGDivider />
        </div>
        <div className="column prn display-inline-block align-t type-semibold">
          <div className="align-button-l plxl prl">{this.props.children}</div>
        </div>
      </div>
    );
  }
}

export default NavItem;

