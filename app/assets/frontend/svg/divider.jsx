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
              <path d="M-13,0 C-19.627417,1.21743675e-15 -25,5.372583 -25,12 L-25,44 C-25,50.627417 -19.627417,56 -13,56 L-15,56 C-21.627417,56 -27,50.627417 -27,44 L-27,12 C-27,5.372583 -21.627417,1.21743675e-15 -15,0 L-13,0 Z M1,0 L3,0 C9.627417,-1.21743675e-15 15,5.372583 15,12 L15,44 C15,50.627417 9.627417,56 3,56 L1,56 C7.627417,56 13,50.627417 13,44 L13,12 C13,5.372583 7.627417,-1.21743675e-15 1,0 Z" fill="currentColor" fillRule="nonzero" mask="url(#mask-2)" />
            </g>
          </g>
        </svg>
      );
    }
}

export default SVGDivider;
