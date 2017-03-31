define(function(require) {
var React = require('react'),
  CSS = require('../lib/css'),
  Event = require('../lib/event');

var BehaviorEditorDropdownMenu = React.createClass({
  displayName: "DropdownMenu",
  propTypes: {
    children: React.PropTypes.node.isRequired,
    labelClassName: React.PropTypes.string,
    label: React.PropTypes.node.isRequired,
    onDownArrow: React.PropTypes.func,
    onUpArrow: React.PropTypes.func,
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
    event.target.blur();
    this.focus();
  },

  onKeyDown: function(event) {
    if (Event.keyPressWasEnter(event) || Event.keyPressWasSpace(event)) {
      this.toggle();
    } else if (Event.keyPressWasUp(event) && this.props.onUpArrow) {
      this.props.onUpArrow();
      event.preventDefault();
    } else if (Event.keyPressWasDown(event) && this.props.onDownArrow) {
      this.props.onDownArrow();
      event.preventDefault();
    }
  },

  onItemKeyDown: function(event) {
    this.onKeyDown(event);
  },

  blur: function() {
    this.refs.button.blur();
  },

  focus: function() {
    this.refs.button.focus();
  },

  componentDidMount: function() {
    // Add click events the old-fashioned way so that propagation up to the document
    // can be stopped. (React events don't bubble up outside of React.)
    this.refs.button.addEventListener('click', this.onClick, false);
    this.refs.button.addEventListener('keydown', this.onKeyDown, false);
    var itemKeys = Object.keys(this.refs).filter(function(key) { return key.match(/^menuItem/); });
    itemKeys.forEach(function(key) {
      this.refs[key].addEventListener('click', this.onItemClick, false);
      this.refs[key].addEventListener('keydown', this.onItemKeyDown, false);
    }, this);
  },

  render: function() {
    // "container" ref is used for testing
    return (
      <div ref="container" className="display-inline-block">
        <button type="button"
          className={
            "button-dropdown-trigger " +
            (this.props.openWhen ? " button-dropdown-trigger-menu-open " : "") +
            (this.props.labelClassName || "")
          }
          ref="button"
          onMouseDown={this.onMouseDown}
        >
          {this.props.label}
        </button>
        <div className="popup-dropdown-container position-relative">
          <ul className={
            "type-s popup popup-dropdown-menu " +
            (this.props.menuClassName || "") +
            (this.props.openWhen ? " fade-in " : " display-none ")
          }>
            {React.Children.map(this.props.children, function(child, index) {
              if (child) {
                return (
                  <li ref={"menuItem" + index} onMouseUp={this.onItemMouseUp} className={child.props.className || ""}>
                    {child}
                  </li>
                );
              } else {
                return null;
              }
            }, this)}
          </ul>
        </div>
      </div>
    );
  }
});

BehaviorEditorDropdownMenu.Item = React.createClass({
  displayName: "DropdownMenuItem",
  propTypes: {
    checkedWhen: React.PropTypes.bool,
    label: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.object]).isRequired,
    onClick: React.PropTypes.func,
    className: React.PropTypes.string
  },

  getInitialState: function() {
    return {
      hover: false
    };
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
  },

  onKeyPress: function(event) {
    if (Event.keyPressWasEnter(event) || Event.keyPressWasSpace(event)) {
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
        <div className="columns columns-elastic">
          {typeof(this.props.checkedWhen) !== 'undefined' ? (
            <div className={"column column-shrink prs align-m " + CSS.visibleWhen(this.props.checkedWhen)}>
              ✓
            </div>
          ) : null}
          <div className={"column column-expand align-m " + (this.props.checkedWhen ? "type-bold" : "")}>
            {this.props.label}
          </div>
        </div>
      </button>
    );
  }
});

return BehaviorEditorDropdownMenu;
});
