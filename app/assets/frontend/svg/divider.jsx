import * as React from 'react';

class SVGDivider extends React.PureComponent {
    render() {
      return (
        <svg width="15px" height="56px" viewBox="0 0 15 56">
          <defs>
            <rect id="path-1" x="0" y="0" width="15" height="56" />
          </defs>
          <g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
            <g id="nav_divider_round">
              <mask id="mask-2" fill="white">
                <use xlinkHref="#path-1" />
              </mask>
              <g id="Mask" />
              <path d="M-28,0 C-34.627417,8.8817842e-16 -40,5.372583 -40,12 L-40,44 C-40,50.627417 -34.627417,56 -28,56 L-1,56 C5.627417,56 11,50.627417 11,44 L11,12 C11,5.372583 5.627417,-1.33226763e-15 -1,0 L-28,0 Z M-28,-4 L-1,-4 C7.836556,-4 15,3.163444 15,12 L15,44 C15,52.836556 7.836556,60 -1,60 L-28,60 C-36.836556,60 -44,52.836556 -44,44 L-44,12 C-44,3.163444 -36.836556,-4 -28,-4 Z" fill="currentColor" fillRule="nonzero" mask="url(#mask-2)" />
            </g>
          </g>
        </svg>
      );
    }
}

export default SVGDivider;
