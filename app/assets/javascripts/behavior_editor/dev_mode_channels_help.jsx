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
            To let you work on a skill without interfering with its users, Ellipsis gives you the ability to only deploy a skill version when you are ready.
          </div>

          <div className="mtl">
            <span>You can test the latest version of a not-yet-deployed skill here in the editor using the <span className="type-bold">Test…</span> button below. </span>
          </div>
          <div className="mtl">
            <div>Additionally, you can enable "dev mode" in any channel in Slack by typing:</div>
            <div className="box-code-example mts">…enable dev mode</div>
            <div className="mts">In this mode, the most recently saved versions of your skills will be available, rather than the most recently deployed ones.</div>
          </div>
          <div className="mtl mbl">
            <span>When you are done, you can put the channel back to normal by typing:</span>
            <div className="box-code-example mts">…disable dev mode</div>
          </div>

        </HelpPanel>
      );
    }
  });

});
