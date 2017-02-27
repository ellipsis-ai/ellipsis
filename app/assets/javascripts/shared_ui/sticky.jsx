define(function(require) {
  var React = require('react');

  function setStyles(element, styles) {
    if (element && element.style) {
      Object.keys(styles).forEach((styleName) => {
        element.style[styleName] = styles[styleName];
      });
    }
  }

  return React.createClass({
    displayName: 'Sticky',
    propTypes: {
      onGetCoordinates: React.PropTypes.func.isRequired,
      children: React.PropTypes.node.isRequired
    },

    resetCoordinates: function() {
      var coords = this.props.onGetCoordinates();

      setStyles(this.placeholder, { width: "" });
      setStyles(this.outerContainer, {
        height: `${coords.bottom}px`
      });
      setStyles(this.innerContainer, {
        position: "static",
        width: "",
        maxHeight: ""
      });

      var newWidth = this.innerContainer.clientWidth;

      setStyles(this.innerContainer, {
        top: `${coords.top}px`,
        left: `${coords.left}px`,
        width: `${newWidth}px`,
        maxHeight: `${coords.bottom}px`,
        position: 'fixed'
      });

      setStyles(this.placeholder, { width: `${newWidth}px` });
    },

    componentDidMount: function() {
      setStyles(this.innerContainer, {
        overflowY: 'auto'
      });

      window.addEventListener('resize', this.resetCoordinates, false);
      this.resetCoordinates();
    },

    componentDidUpdate: function() {
      this.resetCoordinates();
    },

    render: function() {
      return (
        <div style={{ position: "relative" }} ref={(div) => { this.outerContainer = div; }}>
          <div ref={(div) => { this.placeholder = div; }} />
          <div ref={(div) => { this.innerContainer = div; }}>
            {this.props.children}
          </div>
        </div>
      );
    }
  });
});
