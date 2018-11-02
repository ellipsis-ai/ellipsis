import * as React from 'react';

interface Props {
  number?: string,
  children: any
}

class SectionHeading extends React.PureComponent<Props> {
  render() {
    return (
      <h4 className="position-relative mtn mbl">
        {this.props.number ? (
          <span className="box-number bg-blue-medium type-white mrm">{this.props.number}</span>
        ) : null}
        <span>
          {this.props.children}
        </span>
      </h4>
    );
  }
}

export default SectionHeading;
