define(function(require) {
var React = require('react'),
  BehaviorEditorHelpButton = require('./behavior_editor_help_button');

return React.createClass({
  render: function() {
    return (
      <div className="box-help type-s mbm">
        <div className="position-absolute position-top-right ptxs prxs">
          <BehaviorEditorHelpButton onClick={this.props.onCollapseClick} toggled={true} inline={true} />
        </div>

        <h4 className="prl">
          Ellipsis listens for “trigger” phrases to activate behaviors
        </h4>

        <p>
          <span>You can set as many different triggers as you need, and Ellipsis will </span>
          <span>respond to any of them. They can be questions, phrases, words, or even 🤖.</span>
        </p>

        <h5>Fill-in-the-blank parameters</h5>
        <p>
          <span>Triggers may include “fill-in-the-blank” parts to allow for dynamic </span>
          <span>text, and which Ellipsis will send to the behavior for processing </span>
          <span>(as parameters for code) or to repeat back in the response.</span>
        </p>

        <p>
          <span>Add a parameter, e.g. <code className="type-bold">{"{name}"}</code> or <code className="type-bold">{"{date}"}</code>, by putting curly brackets (braces) </span>
          <span>around a parameter name.</span>
        </p>

        <p className="mbn">
          <span>Parameter names must begin with a letter of the alphabet, and may only include </span>
          <span>letters, numbers and underscores (_) — <strong>no spaces.</strong></span>
        </p>

      </div>
    );
  }
});

});