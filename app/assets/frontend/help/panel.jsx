import * as React from 'react';
import CollapseButton from '../shared_ui/collapse_button';

const HelpPanel = React.createClass({
  propTypes: {
    children: React.PropTypes.node.isRequired,
    heading: React.PropTypes.node.isRequired,
    onCollapseClick: React.PropTypes.func.isRequired
  },
  render: function() {
    return (
      <div className="box-help type-s ptn mobile-position-fixed-bottom-full container container-wide mbneg1">
        <div className="columns">
          <div className="column column-page-sidebar mobile-display-none" />
          <div className="column column-page-main position-relative">
            <div className="position-absolute position-top-right">
              <CollapseButton onClick={this.props.onCollapseClick} direction="down" />
            </div>
          </div>
        </div>
        <div className="columns">
          <div className="column column-page-sidebar mtl mobile-prxl">
            {typeof this.props.heading === "string" ? (
              <h4 className="mtn type-weak">
                {this.props.heading}
              </h4>
            ) : this.props.heading}
          </div>
          <div className="column column-page-main prxl">
            <div className="mtl mobile-mtn">
              {React.Children.map(this.props.children, function(child) { return child; })}
            </div>
          </div>
        </div>
      </div>
    );
  }
});

export default HelpPanel;
