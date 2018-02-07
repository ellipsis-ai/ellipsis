import * as React from 'react';
import SVGCollapse from '../svg/collapse';

const CollapseButton = React.createClass({
    propTypes: {
      direction: React.PropTypes.string,
      onClick: React.PropTypes.func.isRequired
    },

    onClick: function() {
      this.props.onClick();
    },

    render: function() {
      return (
        <button type="button" className="button-raw type-weak align-t" onClick={this.onClick} style={{ height: "22px" }}>
          <SVGCollapse direction={this.props.direction} />
        </button>
      );
    }
});

export default CollapseButton;
