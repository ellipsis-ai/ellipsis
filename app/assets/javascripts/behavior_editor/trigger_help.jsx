define(function(require) {
var React = require('react'),
  HelpPanel = require('../help/panel');

return React.createClass({
  propTypes: {
    onCollapseClick: React.PropTypes.func.isRequired
  },
  render: function() {
    return (
      <HelpPanel
        heading="Ellipsis listens for “trigger” phrases to activate behaviors"
        onCollapseClick={this.props.onCollapseClick}
      >
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

        <ul className="list-space-s">
          <li>
            <span>Add a parameter, e.g. <code className="type-bold">{"{name}"}</code> or <code className="type-bold">{"{date}"}</code>, by putting curly brackets (braces) </span>
            <span>around a parameter name.</span>
          </li>

          <li>
            <span>Parameter names must begin with a letter, and otherwise may only include </span>
            <span>letters, numbers, and underscores (_).</span>
          </li>

          <li>
            <span>If your behavior runs code, use the same parameter names in your function </span>
            <span>that you use in your triggers. The function will receive whatever the user typed.</span>
          </li>
        </ul>

        <h5>Trigger options</h5>
        <p>The way triggers are interpreted can be changed with certain options.</p>

        <ul className="list-space-s">
          <li><b className="type-m">🗣 🤖 </b> — if checked, Ellipsis will only respond to this trigger when someone mentions
          @ellipsis or starts their message with “…”.</li>
          <li>
            <b className="type-m"><i>Aa</i></b> — if checked, Ellipsis will only respond if uppercase and lowercase letters match exactly. If
            unchecked, letter case is ignored.
          </li>
          <li>
            <b className="type-m"><code>/^…$/</code></b> — if checked, this trigger will be interpreted as a regular expression
            pattern (regex) instead of normal text. Use regex capturing parentheses
            to collect user input instead of the <code>{"{paramName}"}</code> style.
          </li>
        </ul>

      </HelpPanel>
    );
  }
});

});
