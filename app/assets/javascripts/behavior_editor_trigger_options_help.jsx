define(function(require) {
var React = require('react'),
  BehaviorEditorHelpButton = require('./behavior_editor_help_button');

return React.createClass({
  render: function() {
    return (
      <div className="box-help type-s pts">
        <div className="container position-relative columns phn">

          <div className="column column-one-quarter mts">
            <h4 className="type-weak">
              Options for each trigger
            </h4>
          </div>
          <div className="column column-three-quarters mts pll">

            <div className="position-absolute position-top-right">
              <BehaviorEditorHelpButton onClick={this.props.onCollapseClick} toggled={true} inline={true} />
            </div>

            <p className="mrxxl">
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

          </div>
        </div>
      </div>
    );
  }
});

});