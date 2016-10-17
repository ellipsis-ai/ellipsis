define(function(require) {
var React = require('react');

return React.createClass({
/*
The Collapsible component reveals or collapses its children in the DOM in response
to the boolean value of its revealWhen property, using the max-height CSS property.

Animation speed defaults to 0.25s, or can be set with the animationDuration property,
which should be set with a number (not a string).

Note: to allow for child content to be dynamic in height and to overflow the
bounds, max-height and overflow get cleared after reveal, and reset before collapse.
*/
  displayName: 'Collapsible',
  propTypes: {
    animationDuration: React.PropTypes.number,
    children: React.PropTypes.node.isRequired,
    className: React.PropTypes.string,
    revealWhen: React.PropTypes.bool.isRequired,
    animateInitialRender: React.PropTypes.bool
  },

  getInitialState: function() {
    return {
      isAnimating: false
    };
  },

  animationDurationSeconds: function() {
    return this.props.animationDuration || 0.25;
  },
  after: function(callback) {
    window.setTimeout(function() { callback(); }, 1);
  },
  afterAnimation: function(callback) {
    window.setTimeout(function() { callback(); }, this.animationDurationSeconds() * 1000);
  },

  addTransition: function() {
    this.refs.container.style.transition = 'max-height ' + this.animationDurationSeconds() + 's ease';
  },
  removeTransition: function() {
    this.refs.container.style.transition = null;
  },

  setMaxHeight: function(height) {
    this.refs.container.style.maxHeight = height;
  },
  setOverflow: function(overflow) {
    this.refs.container.style.overflow = overflow;
  },

  setCurrentHeight: function() {
    var c = this.refs.container;
    this.setMaxHeight(c.scrollHeight + 'px');
  },
  setNoHeight: function() {
    this.setMaxHeight('0px');
  },
  setAutoHeight: function() {
    this.setMaxHeight('none');
  },

  setHidden: function() {
    this.refs.container.style.display = 'none';
  },
  setVisible: function() {
    this.refs.container.style.display = null;
  },

  collapse: function() {
    this.setState({
      isAnimating: true
    }, () => {
      this.removeTransition();
      this.setCurrentHeight();
      this.setOverflow('hidden');
      this.after(this.doCollapse);
    });
  },
  doCollapse: function() {
    this.addTransition();
    this.setNoHeight();
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
      isAnimating: true
    }, () => {
      this.setVisible();
      this.addTransition();
      this.setCurrentHeight();
      this.afterAnimation(this.afterReveal);
    });
  },
  afterReveal: function() {
    this.removeTransition();
    this.setAutoHeight();
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

  componentDidUpdate: function(prevProps) {
    /* We can't use shouldComponentUpdate because that prevents children from being re-rendered */
    if (prevProps.revealWhen === this.props.revealWhen) {
      return;
    }
    if (this.state.isAnimating) {
      window.setTimeout(() => {
        this.componentDidUpdate(prevProps);
      }, this.animationDurationSeconds() * 1000);
    } else if (this.props.revealWhen) {
      this.reveal();
    } else {
      this.collapse();
    }
  },

  render: function() {
    return (
      <div ref="container" style={{ maxHeight: '0px', overflow: 'hidden' }} className={this.props.className || ""}>
        {React.Children.map(this.props.children, function(child) { return child; })}
      </div>
    );
  }
});

});
