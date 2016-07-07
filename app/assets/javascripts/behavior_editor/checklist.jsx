define(function(require) {
var React = require('react');

return React.createClass({
  propTypes: {
    children: React.PropTypes.node.isRequired,
    className: React.PropTypes.string,
    disabledWhen: React.PropTypes.bool.isRequired
  },
  render: function() {
    return (
      <ul className={
        "type-s list-space-s checklist " +
        (this.props.disabledWhen ? " type-weak " : "") +
        (this.props.className || "")
      }>
        {React.Children.map(this.props.children, function(child) {
          if (child) {
            return (
              <li className={
                (child.props.checkedWhen ? " checklist-checked " : "") +
                (child.props.hiddenWhen ? " display-none " : "")
              }>{child}</li>
            );
          } else {
            return null;
          }
        })}
      </ul>
    );
  }
});

});
