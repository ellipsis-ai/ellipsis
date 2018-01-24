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
          <div className="mtm">
            <span>To avoid interfering with other users of a skill, Ellipsis allows you to make changes and test them before they are available to everyone. </span>
            <span>Once you feel the skill is ready, you can deploy the latest version to the whole team. </span>
          </div>

          <div className="mtl">
            You can test the latest version of a not-yet-deployed skill in two ways:
          </div>

          <div className="mtl">
            <span className="type-bold">Here in the editor</span> using the <span className="type-monospace">Test…</span> button below.
          </div>

          <div className="mtl">
            <div><span className="type-bold">In Slack</span>, enable dev mode in any channel by typing:</div>
            <div><span className="box-chat box-chat-selected type-white mts">@ellipsis enable dev mode</span></div>
            <div className="mts">In this mode, the most recently saved versions of your skills will be triggered, rather than the most recently deployed ones.</div>
          </div>
          <div className="mtl mbl">
            <span>When you are done, put the channel back to normal by typing:</span>
            <div><span className="box-chat box-chat-selected type-white mts">@ellipsis disable dev mode</span></div>
          </div>

        </HelpPanel>
      );
    }
  });

});
