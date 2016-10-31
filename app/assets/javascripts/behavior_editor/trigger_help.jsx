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
        heading="Ellipsis listens for “trigger” phrases to activate skills"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>You can set as many different triggers as you need, and Ellipsis will </span>
          <span>respond to any of them. They can be questions, phrases, words, or even 🤖.</span>
        </p>

        <h5>Fill-in-the-blank inputs</h5>
        <p>
          <span>Triggers may include “fill-in-the-blank” inputs to allow for dynamic </span>
          <span>text, and which Ellipsis will send to the skill for processing </span>
          <span>(as parameters for code) or to repeat back in the response.</span>
        </p>

        <ul className="list-space-s">
          <li>
            <span>Add an input, e.g. <code className="type-bold">{"{name}"}</code> or <code className="type-bold">{"{date}"}</code>, by putting curly brackets (braces) </span>
            <span>around a label.</span>
          </li>

          <li>
            <span>Input labels can only contain non-accented letters, numbers, dollar signs, or underscores, and </span>
            <span>may not begin with a number.</span>
          </li>

          <li>
            <span>If your skill runs code, any inputs you’ve defined will become parameters in your function. </span>
            <span>The parameter will contain whatever the user typed.</span>
          </li>
        </ul>

        <h5>Speak when spoken to?</h5>
        <p>
          <span>Use the <b>Any message/To Ellipsis</b> option to control whether Ellipsis responds </span>
          <span>to any message containing a phrase in a channel, or only when the message mentions Ellipsis </span>
          <span>by name.</span>
        </p>
        <p>
          <span><b>Shortcut:</b> Start a message with three periods or the </span>
          <span>ellipsis symbol <b>…</b> to mention Ellipsis.</span>
        </p>

        <h5>Trigger phrase interpretation</h5>
        <ul className="list-space-s">
          <li>
            <span><b>Normal phrase (ignore case)</b> — by default, Ellipsis looks for </span>
            <span>a phrase ignoring differences in letter case.</span>
          </li>
          <li>
            <span><b>Case-sensitive phrase</b> — Match phrases only when </span>
            <span>the letter case is exactly the same.</span>
          </li>
          <li>
            <span><b>Regular expression (ignore case)</b> — Interpret a trigger as a </span>
            <a href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html" target="_blank">regular expression pattern</a>
            <span>, ignoring differences in letter case.</span>
          </li>
          <li>
            <p>
              <span><b>Regular expression (case-sensitive)</b> — same as above, but letter case </span>
              <span>must match exactly.</span>
            </p>
            <p>
              <span><b>Note:</b> To include “fill-in-the-blank” inputs in regular expressions, use capturing parentheses </span>
              <span>and character classes instead of the <code>{"{name}"}</code> style, e.g. <code className="box-code-example">add (\d+) plus (\d+)</code></span>
            </p>
          </li>
        </ul>

      </HelpPanel>
    );
  }
});

});
