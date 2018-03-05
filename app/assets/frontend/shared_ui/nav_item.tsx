import * as React from 'react';
import SVGDivider from '../svg/divider';
import {NavItemContent} from '../shared_ui/page';
import Button from "../form/button";

class NavItem extends React.PureComponent<NavItemContent> {
  renderTitle() {
    if (this.props.callback) {
      return (
        <Button className="button-l button-raw pvl plxl prl" onClick={this.props.callback}>{this.props.title}</Button>
      );
    } else if (this.props.url) {
      return (
        <a className="align-button-l plxl prl" href={this.props.url}>{this.props.title}</a>
      );
    } else {
      return (
        <div className="align-button-l plxl prl">{this.props.title}</div>
      );
    }
  }

  render() {
    return (
      <div className="column prn display-nowrap fade-in">
        <div className="display-inline-block align-t type-semibold">
          {this.renderTitle()}
        </div>
        <div className="display-inline-block height-button-l align-t color-black-translucent">
          <SVGDivider />
        </div>
      </div>
    );
  }
}

export default NavItem;

