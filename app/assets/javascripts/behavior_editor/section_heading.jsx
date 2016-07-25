define(function(require) {
var React = require('react');

return React.createClass({
  propTypes: {
    children: React.PropTypes.node.isRequired
  },
  render: function() {
    return (
      <h4 className="position-relative">
        <span className="position-relative position-z-above bg-lightest prl">
          {React.Children.map(this.props.children, function(child) { return child; })}
        </span>
        <div className="arrow-right mobile-display-none"></div>
      </h4>
    );
  }
});

});
