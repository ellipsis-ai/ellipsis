define(function(require) {
var React = require('react'),
  BehaviorEditorMixin = require('./behavior_editor_mixin');

var BehaviorEditorDropdownMenu = React.createClass({
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

  // Next two handlers needed to prevent clicks bubbling to the document which
  // might close an open dropdown

  onClick: function(event) {
    event.stopPropagation();
  },

  onItemClick: function(event) {
    event.stopPropagation();
  },

  onItemMouseUp: function(event) {
    this.toggle();
    this.blur();
  },

  onKeyPress: function(event) {
    if (this.eventKeyPressWasEnter(event) || this.eventKeyPressWasSpace(event)) {
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

  componentDidMount: function() {
    // Add click events the old-fashioned way so that propagation up to the document
    // can be stopped. (React events don't bubble up outside of React.)
    this.refs.button.addEventListener('click', this.onClick, false);
    var itemKeys = Object.keys(this.refs).filter(function(key) { return key.match(/^menuItem/); });
    itemKeys.forEach(function(key) {
      this.refs[key].addEventListener('click', this.onItemClick, false);
    }, this);
  },

  render: function() {
    // "container" ref is used for testing
    return (
      <div ref="container" className="display-inline-block">
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
            {React.Children.map(this.props.children, function(child, index) {
              return (
                <li ref={"menuItem" + index} onMouseUp={this.onItemMouseUp} onKeyPress={this.onItemKeyPress}>
                  {child}
                </li>);
            }, this)}
          </ul>
        </div>
      </div>
    );
  }
});

BehaviorEditorDropdownMenu.Item = React.createClass({
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
    if (this.eventKeyPressWasEnter(event) || this.eventKeyPressWasSpace(event)) {
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

return BehaviorEditorDropdownMenu;
});
