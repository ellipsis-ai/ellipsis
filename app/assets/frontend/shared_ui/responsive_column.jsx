import * as React from 'react';

const ResponsiveColumn = React.createClass({
    propTypes: {
      children: React.PropTypes.node.isRequired
    },

    render: function() {
      return (
        <div className="column column-one-third narrow-column-one-half mobile-column-full phl pbxxl mobile-pbl">
          {this.props.children}
        </div>
      );
    }
});

export default ResponsiveColumn;
