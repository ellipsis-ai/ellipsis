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
            (this.props.size ? "form-multi-select " : "form-select ") +
            (this.state.focused ? "form-select-focus " : "") +
            (this.props.className || "")
          }
        >
          <select ref="select" className={this.props.size ? "form-input form-input-height-auto" : "form-select-element"}
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
