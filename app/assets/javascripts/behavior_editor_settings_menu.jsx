define([
  'react',
  './behavior_editor_mixin'
], function(
  React,
  BehaviorEditorMixin
) {

return React.createClass({
  displayName: 'BehaviorEditorSettingsMenu',
  mixins: [BehaviorEditorMixin],
  render: function() {
    return (
      <div className="position-relative">
        <ul className={"dropdown-menu dropdown-menu-right" + this.visibleWhen(this.props.isVisible)}>
          {React.Children.map(this.props.children, function(child) {
            return (<li onMouseUp={this.props.onItemClick}>{child}</li>);
          }, this)}
        </ul>
      </div>
    );
  }
});

});
