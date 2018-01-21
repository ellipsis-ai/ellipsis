// @flow
define(function(require) {
  var React = require('react'),
    Button = require('../form/button'),
    SVGPlus = require('../svg/plus');

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

  return AddButton;

});

