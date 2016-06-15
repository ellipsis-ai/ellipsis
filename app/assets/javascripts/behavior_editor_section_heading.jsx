if (typeof define !== 'function') { var define = require('amdefine')(module); }
define(function(require) {
var React = require('react');

return React.createClass({
  render: function() {
    return (
      <h4 className="position-relative">
        <span className="position-relative position-raised bg-lightest prl">
          {React.Children.map(this.props.children, function(child) { return child; })}
        </span>
        <div className="arrow-right"></div>
      </h4>
    );
  }
});

});
