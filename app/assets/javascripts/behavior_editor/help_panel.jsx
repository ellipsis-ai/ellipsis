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
      <div className="box-help type-s pts mobile-position-fixed-full">
        <div className="container position-relative columns phn">
          <div className="column column-one-quarter mobile-column-full mts mobile-prxxl">
            <h4 className="type-weak">
              {this.props.heading}
            </h4>
          </div>
          <div className="column column-three-quarters mobile-column-full mts pll prxxl mobile-pln mobile-mtn">
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
