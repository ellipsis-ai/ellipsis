define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin'),
  SVGQuestionMark = require('./svg/question_mark'),
  SVGXIcon = require('./svg/x');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    className: React.PropTypes.string,
    children: React.PropTypes.node,
    inline: React.PropTypes.bool,
    onClick: React.PropTypes.func.isRequired,
    showHelp: React.PropTypes.bool,
    toggled: React.PropTypes.bool
  },
  onClick: function() {
    this.props.onClick();
  },
  render: function() {
    return (
      <span className="position-relative">
        <button type="button"
          ref="button"
          className={
            "button-symbol button-s " +
            (this.props.toggled ? " button-help-toggled " : "") +
            (this.props.inline ? " button-subtle " : "") +
            (this.props.className || "")
          }
          onClick={this.onClick}
        >
          {this.props.toggled ? (<SVGXIcon label="Close" />) : (<SVGQuestionMark />)}
        </button>
        <div className={"position-absolute " + this.visibleWhen(this.props.showHelp)}>
          {React.Children.map(this.props.children, function(child) {
            return child;
          })}
        </div>
      </span>
    );
  }
});

});
