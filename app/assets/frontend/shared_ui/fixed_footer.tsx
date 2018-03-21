import * as React from 'react';
import {default as FixedElement, FixedElementProps} from "./fixed_element";

class FixedFooter extends React.Component<FixedElementProps> {
  render() {
    return (
      <FixedElement
        elementType={"footer"}
        className={this.props.className}
        zIndexClassName={this.props.zIndexClassName}
        onHeightChange={this.props.onHeightChange}
        marginTop={this.props.marginTop}
      >
        {this.props.children}
      </FixedElement>
    );
  }
}

export default FixedFooter;
