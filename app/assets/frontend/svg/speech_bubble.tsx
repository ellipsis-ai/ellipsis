import * as React from 'react';

interface Props {
  label?: Option<string>
}

class SVGSpeechBubble extends React.PureComponent<Props> {
  label() {
    return this.props.label || 'Chat';
  }

  render() {
    return (
      <svg role="img" aria-label={this.label()} height="100%" viewBox="0 0 24 24">
        <title>{this.label()}</title>
        <g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
          <g transform="translate(3.000000, 3.000000)" stroke="currentColor">
            <path
              d="M3.38219111,17.9289222 C4.04224595,17.61615 4.69603046,17.2654056 5.31065694,16.8809006 C6.62088816,16.0612312 7.59963406,15.1825869 8.10980417,14.2834343 L8.25344733,14.0302694 L8.54452427,14.0301786 L11.0020796,14.0294118 C14.5891285,14.0294118 17.5,11.1144423 17.5,7.52005893 L17.5,7.00935283 C17.5,3.41195531 14.5918151,0.5 11.0020796,0.5 L6.99792037,0.5 C3.41087149,0.5 0.5,3.41496949 0.5,7.00935283 L0.5,7.52005893 C0.5,10.0857741 1.99232256,12.376847 4.27621682,13.4339941 L4.64197502,13.6032928 L4.55418379,13.9966549 C4.15685723,15.776938 3.75657337,17.0530096 3.38219111,17.9289222 Z"
            />
          </g>
        </g>
      </svg>
    );
  }
}

export default SVGSpeechBubble;
