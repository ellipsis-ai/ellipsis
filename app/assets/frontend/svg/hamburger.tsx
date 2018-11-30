import * as React from 'react';

interface Props {
  label?: Option<string>
}

class SVGHamburger extends React.PureComponent<Props> {
    label(): string {
      return this.props.label || '≡';
    }
    render() {
      return (
        <svg role="img" aria-label={this.label()} height="100%" viewBox="0 0 16 24">
          <title>{this.label()}</title>
          <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
            <g id="hamburger" fill="currentColor">
              <path d="M0,3 L16,3 L16,6 L0,6 L0,3 Z M0,10 L16,10 L16,13 L0,13 L0,10 Z M0,17 L16,17 L16,20 L0,20 L0,17 Z" id="Combined-Shape" />
            </g>
          </g>
        </svg>
      );
    }
}

export default SVGHamburger;
