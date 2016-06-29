define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      behaviorData: React.PropTypes.object.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      isInstalled: React.PropTypes.bool.isRequired,
      triggers: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    render: function() {
      return (
        <div className="columns columns-elastic mbm">
          <div className="column column-shrink">
            <form action={jsRoutes.controllers.ApplicationController.doImportBehavior().url} method="POST">
              <input type="hidden" name="csrfToken" value={this.props.csrfToken} />
              <input type="hidden" name="teamId" value={this.props.behaviorData.teamId} />
              <input type="hidden" name="dataJson" value={JSON.stringify(this.props.behaviorData)} />
              <button type="submit" className="button-s button-shrink">Install</button>
            </form>
          </div>
          <div className="column column-expand type-s">
            {this.props.triggers.map(function(trigger, index) {
              return (
                <span key={"trigger" + index} className={"type-monospace " + (index > 0 ? "type-weak" : "")}>
                  <span>{trigger.text}</span>
                  {(index < this.props.triggers.length - 1) ? (
                      <span className="type-disabled"> · </span>
                    ) : null
                  }
                </span>

              );
            }, this)}

            {this.props.isInstalled ? (
                <span className="type-weak mls phs bg-green border border-green border-radius">
                  <span className="type-green">✓</span>
                  <span> Installed</span>
                </span>
              ) : null
            }
          </div>
        </div>
      );
    }
  });
});
