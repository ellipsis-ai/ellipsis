define(function(require) {
  var React = require('react'),
    SVGInstall = require('./svg/install'),
    SVGInstalled = require('./svg/installed'),
    SVGInstalling = require('./svg/installing');
    require('es6-promise');
    require('fetch');

  return React.createClass({
    propTypes: {
      behaviorData: React.PropTypes.object.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      isImported: React.PropTypes.bool.isRequired,
      triggers: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      onBehaviorImport: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        importing: false
      }
    },

    getLocalBehaviorEditLink: function() {
      var localBehaviorId = this.getLocalBehaviorId();
      if (localBehaviorId) {
        var url = jsRoutes.controllers.ApplicationController.editBehavior(localBehaviorId).url;
        return (
          <a
            className="mhm"
            href={url}
          >Edit installed version</a>
        );
      } else {
        return null;
      }
    },

    getLocalBehaviorId: function() {
      return this.props.behaviorData.localBehaviorId;
    },

    getInstallButton: function() {
      if (this.isInstalling()) {
        return (
          <button type="button" className="button-raw button-s">
            <SVGInstalling />
          </button>
        );
      } else if (this.behaviorAlreadyImported()) {
        return (
          <button type="button" className="button-raw button-s" onClick={this.importBehavior}>
            <SVGInstalled />
          </button>
        );
      } else {
        return (
          <button type="button" className="button-raw button-s" onClick={this.importBehavior}>
            <SVGInstall />
          </button>
        );
      }
    },

    behaviorAlreadyImported: function() {
      return !!this.props.isImported;
    },

    isInstalling: function() {
      return this.state.importing;
    },

    isFirstTriggerIndex: function(index) {
      return index === 0;
    },

    isLastTriggerIndex: function(index) {
      return index === this.props.triggers.length - 1;
    },

    importBehavior: function() {
      this.setState({
        importing: true
      });
      var headers = new Headers();
      headers.append('x-requested-with', 'XMLHttpRequest');
      fetch(jsRoutes.controllers.ApplicationController.doImportBehavior().url, {
        credentials: 'same-origin',
        headers: headers,
        method: 'POST',
        body: new FormData(this.refs.form)
      }).then(function(response) {
        return response.json()
      }).then(function(json) {
        this.setState({
          importing: false
        });
        this.props.onBehaviorImport(this.props.behaviorData.config.publishedId, json.behaviorId);
      }.bind(this));
    },

    render: function() {
      return (
        <form ref="form" action={jsRoutes.controllers.ApplicationController.doImportBehavior().url} method="POST">
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
              {this.getLocalBehaviorEditLink()}
              {this.behaviorAlreadyImported() ? /* TODO: update/re-install buttons */
                "" : ""}
            </div>
          </div>
        </form>
      );
    }
  });
});
