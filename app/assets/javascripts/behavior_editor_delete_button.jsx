if (typeof define !== 'function') { var define = require('amdefine')(module); }
define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  SVGXIcon = require('./svg/x');

return React.createClass({
  displayName: 'BehaviorEditorDeleteButton',
  mixins: [BehaviorEditorMixin],
  onClick: function(event) {
    this.props.onClick();
    this.refs.button.blur();
  },

  render: function() {
    return (
      <span className="type-weak"><button type="button"
        ref="button"
        className={"button-subtle button-symbol" + this.visibleWhen(!this.props.hidden)}
        onClick={this.onClick}
        title={this.props.title || "Delete"}
      >
        <SVGXIcon label="Delete" />
      </button></span>
    );
  }
});

});
