import * as React from 'react';
import Button from '../form/button';
import SVGPlus from '../svg/plus';

type Props = {
    onClick: () => void,
    label?: string
}

class AddButton extends React.PureComponent<Props> {
    render() {
      return (
        <Button
          onClick={this.props.onClick}
          className="button-s button-subtle button-symbol" title={this.props.label || "Add another"}
        ><SVGPlus /></Button>
      );
    }
}

export default AddButton;
