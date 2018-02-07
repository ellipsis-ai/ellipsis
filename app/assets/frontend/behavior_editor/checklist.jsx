import * as React from 'react';

const Checklist = React.createClass({
    propTypes: {
      children: React.PropTypes.node.isRequired,
      className: React.PropTypes.string,
      disabledWhen: React.PropTypes.bool
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
    displayName: "ChecklistItem",
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

export default Checklist;
