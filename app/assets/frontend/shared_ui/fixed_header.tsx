import * as React from 'react';
import {default as FixedElement, FixedElementProps} from "./fixed_element";

class FixedHeader extends React.Component<FixedElementProps> {
  render() {
    return (
      <FixedElement
        elementType={"header"}
        className={this.props.className}
        onHeightChange={this.props.onHeightChange}
        marginTop={this.props.marginTop}
      >
        {this.props.children}
      </FixedElement>
    );
  }
}

export default FixedHeader;
