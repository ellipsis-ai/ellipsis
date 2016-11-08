define(function(require) {
  var React = require('react'),
    SVGInstall = require('../svg/install'),
    SVGInstalled = require('../svg/installed'),
    SVGInstalling = require('../svg/installing');
    require('whatwg-fetch');

  return React.createClass({
    propTypes: {
      behaviorData: React.PropTypes.object.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      isImported: React.PropTypes.bool.isRequired,
      triggers: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      onBehaviorImport: React.PropTypes.func.isRequired
    },

    getGithubLink: function() {
      if (this.getLocalBehaviorId()) {
        return null;
      } else {
        return (
          <a
            className="mhm fade-in"
            target="_blank"
            href={this.getGithubUrl()}
            >View on Github</a>
        );
      }
    },

    getGithubUrl: function() {
      return this.props.behaviorData.githubUrl;
    },

    getInitialState: function() {
      return {
        importing: false
      };
    },

    getLocalBehaviorEditLink: function() {
      if (this.isImporting()) {
        return (
          <span className="mhm fade-in type-weak type-bold">Installing…</span>
        );
      }
      var localBehaviorId = this.getLocalBehaviorId();
      if (localBehaviorId) {
        var url = jsRoutes.controllers.BehaviorEditorController.edit(localBehaviorId).url;
        return (
          <a
            className="mhm fade-in"
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
      if (this.isImporting()) {
        return (
          <button title="Installing, please wait…" type="button" className="button-raw button-s" disabled="disabled" style={{ width: 40, height: 24 }}>
            <SVGInstalling />
          </button>
        );
      } else if (this.behaviorAlreadyImported()) {
        return (
          <button title="Already installed" type="button" className="button-raw button-s" disabled="disabled" style={{ width: 40, height: 24 }}>
            <SVGInstalled />
          </button>
        );
      } else {
        return (
          <button title="Install this skill" type="button" className="button-raw button-s" onClick={this.importBehavior} style={{ width: 40, height: 24 }}>
            <SVGInstall />
          </button>
        );
      }
    },

    behaviorAlreadyImported: function() {
      return !!this.props.isImported;
    },

    isImporting: function() {
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
      fetch(jsRoutes.controllers.BehaviorImportExportController.doImport().url, {
        credentials: 'same-origin',
        headers: headers,
        method: 'POST',
        body: new FormData(this.refs.form)
      }).then(function(response) {
        return response.json();
      }).then(function(json) {
        this.setState({
          importing: false
        });
        this.props.onBehaviorImport(this.props.behaviorData.config.publishedId, json.behaviorId);
      }.bind(this));
    },

    render: function() {
      return (
        <form ref="form" action={jsRoutes.controllers.BehaviorImportExportController.doImport().url} method="POST">
          <input type="hidden" name="csrfToken" value={this.props.csrfToken} />
          <input type="hidden" name="teamId" value={this.props.behaviorData.teamId} />
          <input type="hidden" name="dataJson" value={JSON.stringify(this.props.behaviorData)} />
          <div className={"columns columns-elastic mbm " + (this.isImporting() ? "pulse" : "")}>
            <div className="column column-shrink">
              {this.getInstallButton()}
            </div>
            <div className="column column-expand type-s">
              <div className="type-italic">{this.props.behaviorData.description || null}</div>
              {this.props.triggers.map(function(trigger, index) {
                return (
                  <span key={"trigger" + index}
                    className={
                      "type-monospace " +
                      (this.isFirstTriggerIndex(index) && !this.props.behaviorData.description ? "" : "type-weak")
                    }
                  >
                    <span className="type-wrap-words">{trigger.text}</span>
                    <span className="type-disabled">
                      {this.isLastTriggerIndex(index) ? "" : " · "}
                    </span>
                  </span>

                );
              }, this)}
              {this.getGithubLink()}
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
