define(function(require) {
  var
    React = require('react'),
    SVGInstall = require('../svg/install'),
    SVGInstalled = require('../svg/installed'),
    SVGInstalling = require('../svg/installing'),
    BehaviorName = require('../behavior_list/behavior_name');

  return React.createClass({
    propTypes: {
      groupData: React.PropTypes.object.isRequired,
      localId: React.PropTypes.string,
      teamId: React.PropTypes.string.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      description: React.PropTypes.string,
      name: React.PropTypes.string.isRequired,
      onBehaviorGroupImport: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        importing: false
      };
    },

    isImporting: function() {
      return this.state.importing;
    },

    isImported: function() {
      return !!this.props.localId;
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
        this.props.onBehaviorGroupImport(json);
      }.bind(this));
    },

    getBehaviors: function() {
      return this.props.groupData.behaviorVersions.filter((version) => !version.isDataType()) || [];
    },

    getGithubLink: function() {
      if (this.isImported()) {
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
      return this.props.groupData.githubUrl;
    },

    getLocalBehaviorEditLink: function() {
      if (this.isImporting()) {
        return (
          <span className="fade-in type-weak type-bold">Installing…</span>
        );
      } else if (this.props.localId) {
        var url = jsRoutes.controllers.BehaviorEditorController.editGroup(this.props.localId).url;
        return (
          <a
            className="fade-in"
            href={url}
          >Edit installed version</a>
        );
      } else {
        return null;
      }
    },

    getInstallButton: function() {
      if (this.isImporting()) {
        return (
          <button title="Installing, please wait…" type="button" className="button-raw mbxs" disabled="disabled" style={{ width: 40, height: 24 }}>
            <SVGInstalling />
          </button>
        );
      } else if (this.isImported()) {
        return (
          <button title="Already installed" type="button" className="button-raw mbxs" disabled="disabled" style={{ width: 40, height: 24 }}>
            <SVGInstalled />
          </button>
        );
      } else {
        return (
          <button title="Install this skill" type="button" className="button-raw mbxs" onClick={this.importBehavior} style={{ width: 40, height: 24 }}>
            <SVGInstall />
          </button>
        );
      }
    },

    render: function() {
      return (
        <form ref="form" action={jsRoutes.controllers.BehaviorImportExportController.doImport().url} method="POST">
          <input type="hidden" name="csrfToken" value={this.props.csrfToken} />
          <input type="hidden" name="teamId" value={this.props.teamId} />
          <input type="hidden" name="dataJson" value={JSON.stringify(this.props.groupData)} />
          <div className={"columns columns-elastic mbm " + (this.isImporting() ? "pulse" : "")}>
              <div className="ptxl mobile-pts">
                <h3 className="border-bottom mtm pbm">
                  <span className="mrl display-inline-block align-t">{this.getInstallButton()}</span>
                  <span className="align-m display-inline-block">{this.props.name}</span>
                  <span className="mrm align-m display-inline-block type-m type-regular type-weak"> &nbsp; · &nbsp; {this.props.description}</span>
                  <span className="mrm align-m display-inline-block type-m">
                    {this.getGithubLink()}
                    {this.getLocalBehaviorEditLink()}
                  </span>
                  {this.isImported() ? /* TODO: update/re-install buttons */
                    "" : ""}
                </h3>
                <div className="type-s display-limit-width">
                {this.getBehaviors().map(function(behavior, index) {
                  return (
                    <BehaviorName
                      key={"behavior" + index}
                      version={behavior}
                      disableLink={true}
                      limitTriggers={true}
                    />
                  );
                }, this)}
                </div>
              </div>
            </div>
          </form>
      );
    }
  });
});
