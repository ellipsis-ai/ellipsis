define(function(require) {
var React = require('react'),
  HelpButton = require('./help_button');

return React.createClass({
  propTypes: {
    children: React.PropTypes.node.isRequired,
    heading: React.PropTypes.node.isRequired,
    onCollapseClick: React.PropTypes.func.isRequired
  },
  render: function() {
    return (
      <div className="box-help type-s pts mobile-position-fixed-full container container-wide mbneg1">
        <div className="position-relative columns">
          <div className="column column-page-sidebar mts mobile-prxxl">
            <h4 className="type-weak">
              {this.props.heading}
            </h4>
          </div>
          <div className="column column-page-main mts prxxl mobile-mtn">
            <div className="position-absolute position-top-right">
              <HelpButton onClick={this.props.onCollapseClick} toggled={true} inline={true} />
            </div>

            {React.Children.map(this.props.children, function(child) { return child; })}
          </div>
        </div>
      </div>
    );
  }
});

});
