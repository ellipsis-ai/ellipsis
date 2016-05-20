define([
  'react',
  './behavior_editor_mixin'
], function(
  React,
  BehaviorEditorMixin
) {

return React.createClass({
  displayName: 'BehaviorEditorDeleteButton',
  mixins: [BehaviorEditorMixin],
  onClick: function(event) {
    this.props.onClick();
    this.refs.button.blur();
  },

  render: function() {
    return (
      <span className="type-weak"><button type="button"
        ref="button"
        className={"button-subtle button-symbol" + this.visibleWhen(!this.props.hidden)}
        onMouseUp={this.onClick}
        title={this.props.title || "Delete"}
      >
        <svg role="img" aria-label="Delete" width="17px" height="24px" viewBox="0 0 17 24">
          <title>Delete</title>
          <g id="Page-1" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
            <g id="delete" fill="currentColor">
              <polygon
                points="3.356 19.968 8.504 14.784 13.652 19.968 16.28 17.304 11.168 12.156 16.28 6.972 13.652 4.308 8.504 9.492 3.356 4.308 0.728 6.972 5.84 12.156 0.728 17.304">
              </polygon>
            </g>
          </g>
        </svg>
      </button></span>
    );
  }
});

});
