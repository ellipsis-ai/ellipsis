import * as React from 'react';
import HelpPanel from '../help/panel';
import URLCreator from '../lib/url_creator';

type Props = {
  onCollapseClick: () => void,
  slackTeamId?: Option<string>,
  botName: string
}

class DevModeChannelsHelp extends React.PureComponent<Props> {
    props: Props;

    renderHeading() {
      return (
        <div>
          <h4 className="mtn type-weak">Deployment and dev mode</h4>

          {this.props.slackTeamId ? (
            <p>
              <a className="button button-shrink button-s" href={URLCreator.forSlack(this.props.slackTeamId)}>Open Slack</a>
            </p>
          ) : null}
        </div>
      );
    }

    render() {
      return (
        <HelpPanel
          heading={this.renderHeading()}
          onCollapseClick={this.props.onCollapseClick}
        >
          <p>
            <span>To avoid impacting other users of a skill, you can make changes </span>
            <span>and test them before they are available to everyone. </span>
            <span>When the updated skill is ready, deploy it to the whole team. </span>
          </p>

          <p>
            Test undeployed skills in two ways:
          </p>

          <ul className="list-space-l">
            <li>
              <p><span className="type-bold">In this editor using the <span className="type-blue-faded">Test…</span> button</span> (on any action)</p>
            </li>
            <li>
              <p className="type-bold">
                <span>In Slack using dev mode:</span>
              </p>

              <p>
                <span>In dev mode, the most recently saved version of each skill will be triggered, </span>
                <span>rather than the deployed version.</span>
              </p>

              <ul>
                <li>
                  <p>
                    <span>Type </span>
                    <span className="box-chat box-chat-help mrs">@{this.props.botName} enable dev mode</span>
                    <span> in any channel to turn it on</span>
                  </p>
                </li>

                <li>
                  <p>
                    <span>Type </span>
                    <span className="box-chat box-chat-help mrs">@{this.props.botName} disable dev mode</span>
                    <span> in any channel to turn it off</span>
                  </p>
                </li>
              </ul>

            </li>
          </ul>

        </HelpPanel>
      );
    }
}

export default DevModeChannelsHelp;
