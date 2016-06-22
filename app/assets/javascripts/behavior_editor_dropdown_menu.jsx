define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    labelClassName: React.PropTypes.string,
    label: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.object]).isRequired,
    openWhen: React.PropTypes.bool.isRequired,
    menuClassName: React.PropTypes.string,
    toggle: React.PropTypes.func.isRequired
  },
  toggle: function() {
    this.props.toggle();
  },

  onMouseDown: function() {
    this.toggle();
  },

  onItemMouseUp: function() {
    this.toggle();
    this.blur();
  },

  onKeyPress: function(event) {
    var ENTER = 13;
    var SPACEBAR = 32;
    if (event.which === ENTER || event.which === SPACEBAR) {
      this.toggle();
    }
  },

  onItemKeyPress: function(event) {
    this.onKeyPress(event);
    this.blur();
  },

  blur: function() {
    this.refs.button.blur();
  },
  render: function() {
    return (
      <div className="display-inline-block">
        <button type="button"
          className={
            "button-dropdown-trigger position-z-popup-trigger " +
            (this.props.openWhen ? " button-dropdown-trigger-menu-open " : "") +
            (this.props.labelClassName || "")
          }
          ref="button"
          onMouseDown={this.onMouseDown}
          onKeyPress={this.onKeyPress}
        >
          {this.props.label}
        </button>
        <div className="position-relative">
          <ul className={
            "type-s popup popup-dropdown-menu " +
            (this.props.menuClassName || "") +
            (this.props.openWhen ? " fade-in " : " display-none ")
          }>
            {React.Children.map(this.props.children, function(child) {
              return (<li onMouseUp={this.onItemMouseUp} onKeyPress={this.onItemKeyPress}>{child}</li>);
            }, this)}
          </ul>
        </div>
      </div>
    );
  }
});

});
