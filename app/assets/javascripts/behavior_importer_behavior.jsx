define(function(require) {
  var React = require('react'),
    SVGInstall = require('./svg/install'),
    SVGInstalled = require('./svg/installed');

  return React.createClass({
    propTypes: {
      behaviorData: React.PropTypes.object.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      isInstalled: React.PropTypes.bool.isRequired,
      triggers: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    getInstallButton: function() {
      if (this.behaviorAlreadyInstalled()) {
        return (
          <button type="button" className="button-raw button-s">
            <SVGInstalled />
          </button>
        );
      } else {
        return (
          <button type="submit" className="button-raw button-s">
            <SVGInstall />
          </button>
        );
      }
    },

    behaviorAlreadyInstalled: function() {
      return !!this.props.isInstalled;
    },

    isFirstTriggerIndex: function(index) {
      return index === 0;
    },

    isLastTriggerIndex: function(index) {
      return index === this.props.triggers.length - 1;
    },

    render: function() {
      return (
        <form action={jsRoutes.controllers.ApplicationController.doImportBehavior().url} method="POST">
          <input type="hidden" name="csrfToken" value={this.props.csrfToken} />
          <input type="hidden" name="teamId" value={this.props.behaviorData.teamId} />
          <input type="hidden" name="dataJson" value={JSON.stringify(this.props.behaviorData)} />
          <div className="columns columns-elastic mbm">
            <div className="column column-shrink">
              {this.getInstallButton()}
            </div>
            <div className="column column-expand type-s">
              {this.props.triggers.map(function(trigger, index) {
                return (
                  <span key={"trigger" + index}
                    className={
                      "type-monospace " +
                      (!this.isFirstTriggerIndex(index) ? "type-weak" : "")
                    }
                  >
                    <span>{trigger.text}</span>
                    <span className="type-disabled">
                      {this.isLastTriggerIndex(index) ? "" : " · "}
                    </span>
                  </span>

                );
              }, this)}

              {this.behaviorAlreadyInstalled() ? (
                <button type="submit" className="mlm button-s button-shrink">Re-install</button>
              ) : ""}
            </div>
          </div>
        </form>
      );
    }
  });
});
