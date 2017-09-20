define(function(require) {
var React = require('react'),
  CollapseButton = require('../shared_ui/collapse_button');

return React.createClass({
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
            <h4 className="mtn type-weak">
              {this.props.heading}
            </h4>
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

});
