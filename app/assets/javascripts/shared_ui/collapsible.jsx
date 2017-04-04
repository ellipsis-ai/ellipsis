define(function(require) {
var React = require('react');

var DEFAULT_DURATION = 0.25;

return React.createClass({
/*
The Collapsible component reveals or collapses its children in the DOM in response
to the boolean value of its revealWhen property, using the max-height CSS property.

Animation speed defaults to 0.25s, or can be set with the animationDuration property,
which should be set with a number (not a string).

Note: to allow for child content to be dynamic in height/width and to overflow the
bounds, max-height/width and overflow get cleared after reveal, and reset before collapse.
*/
  displayName: 'Collapsible',
  propTypes: {
    animationDisabled: React.PropTypes.bool,
    animationDuration: React.PropTypes.number,
    children: React.PropTypes.node.isRequired,
    className: React.PropTypes.string,
    revealWhen: React.PropTypes.bool.isRequired,
    animateInitialRender: React.PropTypes.bool,
    isHorizontal: React.PropTypes.bool,
    onChange: React.PropTypes.func
  },

  timers: [],

  getInitialState: function() {
    return {
      isAnimating: false
    };
  },

  setContainerStyle: function(name, value) {
    if (this.refs.container) {
      this.refs.container.style[name] = value;
    }
  },

  isVertical: function() {
    return !this.props.isHorizontal;
  },

  animationDurationSeconds: function() {
    return this.props.animationDuration || DEFAULT_DURATION;
  },
  animationDurationMilliseconds: function() {
    return this.animationDurationSeconds() * 1000;
  },
  after: function(callback) {
    if (this.props.animationDisabled) {
      callback();
    } else {
      this.timers.push(setTimeout(() => {
        callback();
      }, 1));
    }
  },
  afterAnimation: function(callback) {
    var f = () => {
      callback();
      if (this.props.onChange) {
        this.props.onChange();
      }
    };
    if (this.props.animationDisabled) {
      f();
    } else {
      this.timers.push(setTimeout(f, this.animationDurationMilliseconds()));
    }
  },

  addTransition: function() {
    var propName = this.isVertical() ? 'max-height' : 'max-width';
    this.setContainerStyle("transition", this.props.animationDisabled ?
      null : `${propName} ${this.animationDurationSeconds()}s ease`);
  },
  removeTransition: function() {
    this.setContainerStyle("transition", null);
  },

  setMaxHeight: function(height) {
    this.setContainerStyle("maxHeight", height);
  },
  setMaxWidth: function(width) {
    this.setContainerStyle("maxWidth", width);
  },
  setOverflow: function(overflow) {
    this.setContainerStyle("overflow", overflow);
  },

  setCurrentHeight: function() {
    var c = this.refs.container;
    this.setMaxHeight((c ? c.scrollHeight : 0) + 'px');
  },
  setNoHeight: function() {
    this.setMaxHeight('0px');
  },
  setAutoHeight: function() {
    this.setMaxHeight('none');
  },

  setCurrentWidth: function() {
    var c = this.refs.container;
    this.setMaxWidth((c ? c.scrollWidth : 0) + 'px');
  },
  setNoWidth: function() {
    this.setMaxWidth('0px');
  },
  setAutoWidth: function() {
    this.setMaxWidth('none');
  },

  setHidden: function() {
    this.setContainerStyle("display", 'none');
  },
  setVisible: function() {
    this.setContainerStyle("display", null);
  },

  collapse: function() {
    this.setState({
      isAnimating: !this.props.animationDisabled
    }, () => {
      this.removeTransition();
      if (this.isVertical()) {
        this.setCurrentHeight();
      } else {
        this.setCurrentWidth();
      }
      this.setOverflow('hidden');
      this.after(this.doCollapse);
    });
  },
  doCollapse: function() {
    this.addTransition();
    if (this.isVertical()) {
      this.setNoHeight();
    } else {
      this.setNoWidth();
    }
    this.afterAnimation(this.finishCollapse);
  },
  finishCollapse: function() {
    this.setHidden();
    this.setState({
      isAnimating: false
    });
  },

  reveal: function() {
    this.setState({
      isAnimating: !this.props.animationDisabled
    }, () => {
      this.setVisible();
      this.addTransition();
      if (this.isVertical()) {
        this.setCurrentHeight();
      } else {
        this.setCurrentWidth();
      }
      this.afterAnimation(this.afterReveal);
    });
  },
  afterReveal: function() {
    this.removeTransition();
    if (this.isVertical()) {
      this.setAutoHeight();
    } else {
      this.setAutoWidth();
    }
    this.setOverflow('visible');
    this.setState({
      isAnimating: false
    });
  },

  componentDidMount: function() {
    if (this.props.animateInitialRender && this.props.revealWhen) {
      this.reveal();
    } else if (this.props.revealWhen) {
      this.afterReveal();
    } else {
      this.finishCollapse();
    }
    this.addTransition();
  },

  shouldComponentUpdate: function(newProps) {
    // Avoid re-rendering costs when the component is staying collapsed
    return newProps.revealWhen || this.props.revealWhen;
  },

  componentDidUpdate: function(prevProps) {
    if (prevProps.revealWhen === this.props.revealWhen) {
      return;
    }
    if (this.state.isAnimating) {
      this.timers.push(setTimeout(() => {
        this.componentDidUpdate(prevProps);
      }, this.animationDurationMilliseconds()));
    } else if (this.props.revealWhen) {
      this.reveal();
    } else {
      this.collapse();
    }
  },

  componentWillUnmount: function() {
    this.timers.forEach((timer) => clearTimeout(timer));
  },

  getDefaultStyle: function() {
    var styles = { overflow: 'hidden' };
    styles[this.isVertical() ? 'maxHeight' : 'maxWidth'] = '0px';
    return styles;
  },

  render: function() {
    return (
      <div ref="container" style={this.getDefaultStyle()} className={this.props.className || ""}>
        {this.props.children}
      </div>
    );
  }
});

});
