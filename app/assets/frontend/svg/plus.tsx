import * as React from 'react';

type Props = {
  label?: string
}

class SVGPlus extends React.PureComponent<Props> {
    label(): string {
      return this.props.label || '+';
    }

    render() {
      return (
        <svg role="img" aria-label={this.label()} height="100%" viewBox="0 0 18 24">
          <title>{this.label()}</title>
          <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
            <g id="plus" fill="currentColor">
              <polygon id="+" points="7.12173913 19 10.8782609 19 10.8782609 12.7627119 17 12.7627119 17 9.23728814 10.8782609 9.23728814 10.8782609 3 7.12173913 3 7.12173913 9.23728814 1 9.23728814 1 12.7627119 7.12173913 12.7627119" />
            </g>
          </g>
        </svg>
      );
    }
}

export default SVGPlus;
