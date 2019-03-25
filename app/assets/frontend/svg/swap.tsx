import * as React from 'react';

class SVGSwap extends React.PureComponent {
    render() {
      return (
        <svg role="img" aria-label={"↑↓ Swap"} height="100%" viewBox="0 0 24 24">
          <title>↑↓ Swap</title>
          <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
            <g id="swap" fill="currentColor">
              <polygon id="↑" transform="translate(6.028687, 12.146545) rotate(-90.000000) translate(-6.028687, -12.146545) " points="12.1752319 12.1422729 7.17645264 17.1752319 3.90374756 17.1752319 7.80023193 13.3214722 -0.117858887 13.3214722 -0.117858887 10.9716187 7.80023193 10.9716187 3.90374756 7.11785889 7.17645264 7.11785889" />
              <polygon id="↓" transform="translate(18.028687, 12.146545) rotate(-270.000000) translate(-18.028687, -12.146545) " points="24.1752319 12.1422729 19.1764526 17.1752319 15.9037476 17.1752319 19.8002319 13.3214722 11.8821411 13.3214722 11.8821411 10.9716187 19.8002319 10.9716187 15.9037476 7.11785889 19.1764526 7.11785889" />
            </g>
          </g>
        </svg>
      );
    }
}

export default SVGSwap;
