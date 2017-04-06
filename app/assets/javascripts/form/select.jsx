define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'Select',
    propTypes: {
      className: React.PropTypes.string,
      name: React.PropTypes.string,
      value: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.number]),
      children: React.PropTypes.node.isRequired,
      onChange: React.PropTypes.func.isRequired,
      size: React.PropTypes.string
    },

    componentDidUpdate: function(prevProps) {
      if (this.props.size && prevProps.value !== this.props.value) {
        this.scrollToSelectedOption();
      }
    },

    scrollToSelectedOption: function() {
      const selector = this.refs.select;
      const selectedOptions = selector.selectedOptions;
      const firstSelected = selectedOptions.length > 0 ? selectedOptions[0] : null;
      if (!firstSelected) {
        return;
      }

      const selectorTop = selector.scrollTop;
      const selectorBottom = selectorTop + selector.offsetHeight;
      const optionTop = firstSelected.offsetTop;
      const optionBottom = optionTop + firstSelected.offsetHeight;
      if (optionBottom < selectorTop || optionTop > selectorBottom) {
        selector.scrollTop = optionTop;
      }
    },

    onChange: function(event) {
     this.props.onChange(event.target.value, event.target.selectedIndex);
    },

    onFocus: function() {
      this.setState({ focused: true });
    },

    onBlur: function() {
      this.setState({ focused: false });
    },

    getInitialState: function() {
      return {
        focused: false
      };
    },

    render: function() {
      return (
        <div
          className={
            "position-relative " + // enables
            (this.props.size ? "form-multi-select " : "form-select ") +
            (this.state.focused ? "form-select-focus " : "") +
            (this.props.className || "")
          }
        >
          <select ref="select"
            className={this.props.size ?
              "form-input form-input-height-auto position-relative" :
              "form-select-element"}
            name={this.props.name}
            value={this.props.value}
            onChange={this.onChange}
            onFocus={this.onFocus}
            onBlur={this.onBlur}
            size={this.props.size}
          >
            {this.props.children}
          </select>
        </div>
      );
    }
  });
});
