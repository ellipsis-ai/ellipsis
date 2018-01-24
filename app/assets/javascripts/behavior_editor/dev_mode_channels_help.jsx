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
          heading="Dev mode channels"
          onCollapseClick={this.props.onCollapseClick}
        >
          <p>
            <span>To avoid impacting other users of a skill, Ellipsis allows you to make changes </span>
            <span>and test them before they are available to everyone. </span>
            <span>When the updated skill is ready, you can deploy it to the whole team. </span>
          </p>

          <p>
            You can test an undeployed skill in two ways:
          </p>

          <ul className="list-space-l">
            <li>
              <p><span className="type-bold">In this editor using the <span className="type-blue-faded">Test…</span> button</span> (on any action)</p>
            </li>
            <li>
              <p><span className="type-bold">In Slack using dev mode:</span></p>

              <p>
                <span>In dev mode, the most recently saved version of each skill will be triggered, </span>
                <span>rather than the deployed version.</span>
              </p>

              <ul>
                <li>
                  <p>
                    <span>Type </span>
                    <span className="box-chat box-chat-help mrs">@ellipsis enable dev mode</span>
                    <span> in any channel to turn it on</span>
                  </p>
                </li>

                <li>
                  <p>
                    <span>Type </span>
                    <span className="box-chat box-chat-help mrs">@ellipsis disable dev mode</span>
                    <span> in any channel to turn it off</span>
                  </p>
                </li>
              </ul>
            </li>
          </ul>

        </HelpPanel>
      );
    }
  });

});
