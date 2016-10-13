define(function(require) {
  var React = require('react'),
    ifPresent = require('../if_present');

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

    getInitialState: function() {
      return {
        minWidth: null
      };
    },

    adjustMinWidthIfNecessary: function() {
      var newMax = this.getMaxLabelWidth();
      if (newMax !== this.state.minWidth) {
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
      Object.keys(this.refs).forEach((refName) => {
        if (/^buttonLabel/.test(refName)) {
          maxWidth = Math.max(maxWidth, this.refs[refName].scrollWidth);
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
      return this.props.labels.map((label, index) => (
        <div ref={`buttonLabel${index}`} key={`buttonLabel${index}`} className={
          `button-label-absolute visibility ${label.displayWhen ? "visibility-visible" : "visibility-hidden"}`
        }>
          {ifPresent(label.mobileText, () => (
            <span>
              <span className="mobile-display-none">{label.text}</span>
              <span className="mobile-display-only">{label.mobileText}</span>
            </span>
          ), () => (
            <span>{label.text}</span>
          ))}
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
