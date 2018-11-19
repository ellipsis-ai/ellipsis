import * as React from 'react';
import HelpPanel from '../help/panel';
import * as Constants from '../lib/constants';

interface Props {
  onCollapseClick: () => void
}

class TriggerHelp extends React.PureComponent<Props> {
  render() {
    return (
      <HelpPanel
        heading="Ellipsis listens for “trigger” phrases to activate skills"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>Triggers are the text that someone types in chat to trigger this action.</span>
        </p>

        <p>
          <span>You can set as many different triggers as you need, and Ellipsis will </span>
          <span>respond to any of them. They can be questions, phrases, words, or even emoji like 🤖.</span>
        </p>

        <p>
          <span>For more flexibility, you can also use <b>regular expression</b> triggers that use pattern matching.</span>
        </p>

        <p>
          <span>Normally, triggers are only recognized when someone mentions Ellipsis in chat, but you can also set triggers </span>
          <span>that will work in any message (as long as Ellipsis has been invited to the channel).</span>
        </p>

        <p>
          <a href={Constants.DOCS_URLS.TRIGGERS} target="help">More info</a>
        </p>

      </HelpPanel>
    );
  }
}

export default TriggerHelp;
