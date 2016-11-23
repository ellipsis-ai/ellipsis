define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'Select',
    propTypes: {
      containerClassName: React.PropTypes.string,
      className: React.PropTypes.string,
      name: React.PropTypes.string,
      value: React.PropTypes.string,
      children: React.PropTypes.node.isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    onChange: function(event) {
     this.props.onChange(event.target.value, event.target.selectedIndex);
    },

    render: function() {
      return (
        <div className={`display-inline-block ${this.props.containerClassName || ""}`}>
          <select className={`form-select ${this.props.className || ""}`}
            name={this.props.name}
            value={this.props.value}
            onChange={this.onChange}
          >
            {this.props.children}
          </select>
        </div>
      );
    }
  });
});
