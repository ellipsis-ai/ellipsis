define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'Collapsible',

/*
The Collapsible component reveals or collapses its children in the DOM in response
to the boolean value of its revealWhen property, using the max-height CSS property.

Animation speed defaults to 0.25s, or can be set with the animationDuration property,
which should be set with a number (not a string).

Note: to allow for child content to be dynamic in height and to overflow the
bounds, max-height and overflow get cleared after reveal, and reset before collapse.
*/

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
    this.removeTransition();
    this.setCurrentHeight();
    this.setOverflow('hidden');
    this.after(this.doCollapse);
  },
  doCollapse: function() {
    this.addTransition();
    this.setNoHeight();
    this.afterAnimation(this.finishCollapse);
  },
  finishCollapse: function() {
    this.setHidden();
  },

  reveal: function() {
    this.setVisible();
    this.addTransition();
    this.setCurrentHeight();
    this.afterAnimation(this.afterReveal);
  },
  afterReveal: function() {
    this.removeTransition();
    this.setAutoHeight();
    this.setOverflow('visible');
  },

  componentDidMount: function() {
    if (this.props.revealWhen) {
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
    if (this.props.revealWhen) {
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
