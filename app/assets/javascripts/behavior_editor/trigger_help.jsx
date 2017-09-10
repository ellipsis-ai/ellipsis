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
        <p className="type-m">
          <span>You can set as many different triggers as you need, and Ellipsis will </span>
          <span>respond to any of them. They can be questions, phrases, words, or even 🤖.</span>
        </p>

        <h5>How trigger phrases are matched</h5>
        <ul className="list-space-s">
          <li>
            <span><b>Normal phrase</b> — by default, Ellipsis looks for </span>
            <span>any message that begins with the phrase (ignoring differences in letter case).</span>
          </li>
          <li>
            <p>
              <span><b>Regular expression</b> — Interpret a trigger as a </span>
              <a href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html" target="_blank">regular expression pattern</a>
              <span>.</span>
            </p>
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

        <h5>Collecting input with fill-in-the-blanks</h5>
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

        <h5>Regular expressions: advanced features</h5>
        <p>
          <span>The case-insensitive flag is turned on by default. Add the prefix </span>
          <span><code>(?-i)</code> to your pattern to force case-sensitive matching.</span>
        </p>

        <p>
          <span>To gather inputs from regular expression triggers, use capturing parentheses </span>
          <span>instead of the <code>{"{name}"}</code> style, e.g. <code className="box-code-example">add (\d+) plus (\d+)</code></span>
        </p>

      </HelpPanel>
    );
  }
});

});
