define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin');

return React.createClass({
  mixins: [BehaviorEditorMixin],
  propTypes: {
    checkedWhen: React.PropTypes.bool,
    label: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.object]).isRequired,
    onClick: React.PropTypes.func
  },

  getInitialState: function() {
    return {
      hover: false
    }
  },

  hasHover: function() {
    return this.state.hover;
  },

  onMouseEnter: function() {
    this.setState({
      hover: true
    });
  },

  onMouseLeave: function() {
    this.setState({
      hover: false
    });
  },

  onMouseUp: function() {
    if (this.props.onClick) {
      this.props.onClick();
    }
    this.refs.button.blur();
  },

  onKeyPress: function(event) {
    var ENTER = 13;
    var SPACEBAR = 32;
    if (event.which === ENTER || event.which === SPACEBAR) {
      this.onMouseUp();
    }
  },

  render: function() {
    return (
      <button
        ref="button"
        type="button"
        className={"button-dropdown-item " + (this.hasHover() ? "button-dropdown-item-hover" : "")}
        onMouseUp={this.onMouseUp}
        onKeyPress={this.onKeyPress}
        onMouseEnter={this.onMouseEnter}
        onMouseLeave={this.onMouseLeave}
      >
        {typeof(this.props.checkedWhen) !== 'undefined' ?
          (<span className={"mrxs " + this.visibleWhen(this.props.checkedWhen)}>✓</span>) :
          null
        }
        <span className={this.props.checkedWhen ? "type-bold" : ""}>{this.props.label}</span>
      </button>
    );
  }
});

});
