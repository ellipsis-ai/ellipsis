define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'DynamicLabelButton',
    propTypes: {
      type: React.PropTypes.string,
      className: React.PropTypes.string,
      disabledWhen: React.PropTypes.bool,
      onClick: React.PropTypes.func,
      labels: React.PropTypes.arrayOf(React.PropTypes.shape({
        text: React.PropTypes.string.isRequired,
        mobileText: React.PropTypes.string,
        displayWhen: React.PropTypes.bool.isRequired
      }))
    },

    buttonLabels: [],

    getInitialState: function() {
      return {
        minWidth: null
      };
    },

    adjustMinWidthIfNecessary: function() {
      var newMax = this.getMaxLabelWidth();
      if (newMax !== this.state.minWidth && newMax > 0) {
        this.setState({ minWidth: newMax });
      }
    },

    componentDidMount: function() {
      this.adjustMinWidthIfNecessary();
    },

    componentDidUpdate: function() {
      this.adjustMinWidthIfNecessary();
    },

    getMaxLabelWidth: function() {
      var maxWidth = 0;
      this.buttonLabels.forEach((label) => {
        if (label && label.scrollWidth) {
          maxWidth = Math.max(maxWidth, label.scrollWidth);
        }
      });
      return maxWidth > 0 ? maxWidth : null;
    },

    getPlaceholderStyle: function() {
      return this.state.minWidth ? ({
        minWidth: `${this.state.minWidth}px`
      }) : null;
    },

    getLabels: function() {
      this.buttonLabels = [];
      return this.props.labels.map((label, index) => (
        <div ref={(element) => this.buttonLabels.push(element)} key={`buttonLabel${index}`} className={
          `button-label-absolute visibility ${label.displayWhen ? "visibility-visible" : "visibility-hidden"}`
        }>
          {label.mobileText ? (
            <span>
              <span className="mobile-display-none">{label.text}</span>
              <span className="mobile-display-only">{label.mobileText}</span>
            </span>
          ) : (
            <span>{label.text}</span>
          )}
        </div>
      ));
    },

    focus: function() {
      this.refs.button.focus();
    },

    render: function() {
      return (
        <button
          ref="button"
          type={this.props.type || "button"}
          className={this.props.className || ""}
          disabled={this.props.disabledWhen || false}
          onClick={this.props.onClick || null}
        >
          <div className="button-labels">
            <div style={this.getPlaceholderStyle()}>&nbsp;</div>
            {this.getLabels()}
          </div>
        </button>
      );
    }
  });
});
