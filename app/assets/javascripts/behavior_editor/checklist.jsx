define(function(require) {
  var React = require('react');

  var Checklist = React.createClass({
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
            return child;
          })}
        </ul>
      );
    }
  });

  Checklist.Item = React.createClass({
    propTypes: {
      children: React.PropTypes.node.isRequired,
      checkedWhen: React.PropTypes.bool,
      hiddenWhen: React.PropTypes.bool
    },
    render: function() {
      return (
        <li className={
          (this.props.checkedWhen ? " checklist-checked " : "") +
          (this.props.hiddenWhen ? " display-none " : " fade-in ")
        }>
          {React.Children.map(this.props.children, function(child) {
            return child;
          })}
        </li>
      );
    }
  });

  return Checklist;
});
