define(function(require) {
  var React = require('react');

  var ToggleGroup = React.createClass({
    propTypes: {
      children: React.PropTypes.node.isRequired,
      className: React.PropTypes.string
    },

    render: function() {
      return (
        <div className={"form-toggle-group " + (this.props.className || "")}>
          {React.Children.map(this.props.children, child => child)}
        </div>
      );
    }
  });

  ToggleGroup.Item = React.createClass({
    propTypes: {
      activeWhen: React.PropTypes.bool.isRequired,
      label: React.PropTypes.node.isRequired,
      onClick: React.PropTypes.func.isRequired,
      title: React.PropTypes.string
    },

    render: function() {
      return (
        <button type="button" className={
          "button-toggle " +
          (this.props.activeWhen ? " button-toggle-active " : "")
        } onClick={this.props.onClick}
          title={this.props.title || ""}
        >{this.props.label}</button>
      );
    }
  });

  return ToggleGroup;
});
