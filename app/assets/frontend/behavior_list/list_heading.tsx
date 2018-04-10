import * as React from 'react';

type Props = {
  heading: any,
  sideContent?: any
};

class ListHeading extends React.PureComponent<Props> {
    render() {
      return (
        <div className="columns columns-elastic mobile-columns-float">
          <div className="column column-expand align-m">
            <h3 className="type-blue-faded mvn mhl mobile-mbm">{this.props.heading}</h3>
          </div>
          {this.props.sideContent ? (
            <div className="column column-shrink align-m phl mobile-pbl">
              {this.props.sideContent}
            </div>
          ) : null}
        </div>
      );
    }
}

export default ListHeading;
