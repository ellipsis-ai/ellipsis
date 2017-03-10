define(function(require) {
  var React = require('react'),
    DeepEqual = require('../lib/deep_equal');

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
      children: React.PropTypes.node.isRequired,
      disabledWhen: React.PropTypes.bool,
      innerClassName: React.PropTypes.string,
      outerClassName: React.PropTypes.string
    },

    scrollTopPosition: 0,

    lastCoords: {},

    resetCoordinates: function() {
      if (!this.innerContainer) {
        return;
      }

      var coords = this.props.onGetCoordinates();
      if (DeepEqual.isEqual(coords, this.lastCoords)) {
        return;
      }

      this.lastCoords = coords;

      setStyles(this.placeholder, { width: "" });
      setStyles(this.outerContainer, { height: "" });
      setStyles(this.innerContainer, {
        position: "static",
        width: "",
        maxHeight: ""
      });

      var newWidth = this.innerContainer.clientWidth;

      if (!this.props.disabledWhen) {
        setStyles(this.outerContainer, {
          height: `${coords.bottom}px`
        });
        setStyles(this.innerContainer, {
          top: `${coords.top}px`,
          left: `${coords.left}px`,
          width: `${newWidth}px`,
          maxHeight: `${coords.bottom}px`,
          position: 'fixed'
        });

        setStyles(this.placeholder, { width: `${newWidth}px` });
      }

      this.innerContainer.scrollTop = this.scrollTopPosition;
    },

    componentDidMount: function() {
      setStyles(this.innerContainer, {
        overflowY: 'auto'
      });

      window.addEventListener('resize', this.resetCoordinates, false);
      this.resetCoordinates();
    },

    componentWillUpdate: function() {
      this.scrollTopPosition = this.innerContainer ? this.innerContainer.scrollTop : 0;
    },

    componentDidUpdate: function(prevProps) {
      if (this.props.disabledWhen && prevProps.disabledWhen) {
        return;
      }
      this.resetCoordinates();
    },

    render: function() {
      return (
        <div className={this.props.outerClassName || ""} style={{ position: "relative" }} ref={(div) => { this.outerContainer = div; }}>
          <div ref={(div) => { this.placeholder = div; }} />
          <div className={this.props.innerClassName || ""} ref={(div) => { this.innerContainer = div; }}>
            {this.props.children}
          </div>
        </div>
      );
    }
  });
});
