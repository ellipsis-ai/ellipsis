import * as React from 'react';

interface Props {
  children: React.ReactNode
}

class ResponsiveColumn extends React.Component<Props> {
    render() {
      return (
        <div className="column column-one-third narrow-column-one-half mobile-column-full phl pbxxl mobile-pbl">
          {this.props.children}
        </div>
      );
    }
}

export default ResponsiveColumn;
