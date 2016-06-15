if (typeof define !== 'function') { var define = require('amdefine')(module); }
define(function(require) {
var React = require('react'),
  BehaviorEditorHelpPanel = require('./behavior_editor_help_panel');

return React.createClass({
  render: function() {
    return (
      <BehaviorEditorHelpPanel
        heading="Options for each trigger"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <b>🗣 🤖 :</b> if checked, Ellipsis will only respond to this trigger when someone mentions
          Ellipsis by name.
        </p>
        <p>
          <b><i>Aa:</i></b> if checked, Ellipsis will match uppercase and case letters exactly. If
          unchecked, case is ignored.
        </p>
        <p>
          <b><code>/^…$/</code>:</b> if checked, this trigger will use regular expression
          pattern matching (regex) instead of normal text. Use regex capturing parentheses
          to collect user input instead of the <code>{"{paramName}"}</code> style.
        </p>
      </BehaviorEditorHelpPanel>
    );
  }
});

});
