define(function(require) {
  const React = require('react');

  class DynamicLabelButton extends React.Component {
    constructor(props) {
      super(props);
      this.buttonLabels = [];
      this.state = {
        minWidth: null
      };
      this.onClick = this.onClick.bind(this);
    }

    adjustMinWidthIfNecessary() {
      var newMax = this.getMaxLabelWidth();
      if (newMax !== this.state.minWidth && newMax > 0) {
        this.setState({ minWidth: newMax });
      }
    }

    componentDidMount() {
      this.adjustMinWidthIfNecessary();
    }

    componentDidUpdate() {
      this.adjustMinWidthIfNecessary();
    }

    getMaxLabelWidth() {
      var maxWidth = 0;
      this.buttonLabels.forEach((label) => {
        if (label && label.scrollWidth) {
          maxWidth = Math.max(maxWidth, label.scrollWidth);
        }
      });
      return maxWidth > 0 ? maxWidth : null;
    }

    getPlaceholderStyle() {
      return this.state.minWidth ? ({
        minWidth: `${this.state.minWidth}px`
      }) : null;
    }

    getLabels() {
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
    }

    focus() {
      if (this.button) {
        this.button.focus();
      }
    }

    onClick() {
      if (this.props.onClick) {
        this.props.onClick();
      }
    }

    render() {
      return (
        <button
          ref={(el) => this.button = el}
          type={this.props.type || "button"}
          className={this.props.className || ""}
          disabled={this.props.disabledWhen || false}
          onClick={this.onClick}
        >
          <div className="button-labels">
            <div style={this.getPlaceholderStyle()}>&nbsp;</div>
            {this.getLabels()}
          </div>
        </button>
      );
    }
  }

  DynamicLabelButton.propTypes = {
    type: React.PropTypes.string,
    className: React.PropTypes.string,
    disabledWhen: React.PropTypes.bool,
    onClick: React.PropTypes.func.isRequired,
    labels: React.PropTypes.arrayOf(React.PropTypes.shape({
      text: React.PropTypes.string.isRequired,
      mobileText: React.PropTypes.string,
      displayWhen: React.PropTypes.bool.isRequired
    })).isRequired
  };

  return DynamicLabelButton;
});
