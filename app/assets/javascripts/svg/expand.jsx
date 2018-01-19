// @flow
define(function(require) {
  const React = require('react');

  type Props = {
    expanded: boolean
  }

  class SVGExpand extends React.PureComponent<Props> {
    label() {
      return this.props.expanded ? "Collapse" : "Expand";
    }

    transform() {
      return this.props.expanded ? "" : "translate(8.000000, 11.000000) rotate(-90.000000) translate(-8.000000, -11.000000)";
    }

    render() {
      return (
        <svg role="img" aria-label={this.label()} height="100%" viewBox="0 0 18 24">
          <title>{this.label()}</title>
          <g id="expandable" fill="currentColor">
            <polygon id="Triangle" transform={this.transform()} points="8 16 0 8 16 8" />
          </g>
        </svg>
      );
    }
  }

  return SVGExpand;

});
