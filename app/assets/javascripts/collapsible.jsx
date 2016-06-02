define(function(require) {
var React = require('react');

return React.createClass({
  displayName: 'CollapsibleContainer',
  animationDurationSeconds: function() {
    return this.props.animationDuration || 0.25;
  },
  debugHeight: function(caller) {
    console.log(caller + ' max height: ' + this.refs.container.style.maxHeight);
  },
  after: function(callback, waitForAnimation) {
    var delay = waitForAnimation ? this.animationDurationSeconds() * 1000 : 1;
    window.setTimeout(function() { callback(); }, delay);
  },
  addTransition: function() {
    this.refs.container.style.transition = 'max-height 0.25s ease-out';
  },
  removeTransition: function() {
    this.refs.container.style.transition = null;
  },
  setCurrentHeight: function() {
    var c = this.refs.container;
    var s = c.style;
    var newHeight = c.scrollHeight
    s.maxHeight = newHeight + 'px';
  },
  setNoHeight: function() {
    var c = this.refs.container;
    var s = c.style;
    s.maxHeight = '0px';
  },
  setAutoHeight: function() {
    var c = this.refs.container;
    var s = c.style;
    s.maxHeight = 'none';
  },
  finishCollapse: function() {
    this.setNoHeight();
  },
  collapse: function() {
    this.setCurrentHeight();
    this.addTransition();
    this.after(this.finishCollapse);
  },
  reveal: function() {
    this.addTransition();
    this.after(this.prepareReveal, true);
  },
  prepareReveal: function() {
    this.setCurrentHeight();
    this.after(this.afterReveal, true);
  },
  afterReveal: function() {
    this.setAutoHeight();
  },
  componentDidUpdate: function(prevProps) {
    if (prevProps && prevProps.revealWhen === this.props.revealWhen) {
      return;
    }
    if (this.props.revealWhen) {
      this.reveal();
    } else {
      this.collapse();
    }
  },
  componentDidMount: function() {
    if (this.props.animateOnRender) {
      this.addTransition();
      this.after(this.componentDidUpdate);
    } else if (this.props.revealWhen) {
      this.setAutoHeight();
    } else {
      this.setNoHeight();
    }
  },
  render: function() {
    return (
      <div ref="container" style={{ maxHeight: '0px', overflow: 'hidden' }}>
        {React.Children.map(this.props.children, function(child) { return child; })}
      </div>
    );
  }
});

});