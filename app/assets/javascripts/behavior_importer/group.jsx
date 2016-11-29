define(function(require) {
  var
    React = require('react'),
    SVGInstall = require('../svg/install'),
    SVGInstalled = require('../svg/installed'),
    SVGInstalling = require('../svg/installing'),
    Behavior = require('./behavior');

  require('whatwg-fetch');

  return React.createClass({
    propTypes: {
      groupData: React.PropTypes.object.isRequired,
      teamId: React.PropTypes.string.isRequired,
      behaviors: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      isImported: React.PropTypes.bool.isRequired,
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

    behaviorAlreadyImported: function() {
      return !!this.props.isImported;
    },

    behaviorIsImported: function(behavior) {
      if (!behavior.config || !behavior.config.publishedId) {
        return false;
      }
      return this.props.checkImported(behavior.config.publishedId);
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
        this.props.onBehaviorGroupImport(this.props.groupData.config.publishedId, json.behaviorId);
      }.bind(this));
    },

    getBehaviors: function() {
      return this.props.behaviors || [];
    },

    getGithubLink: function() {
      if (false/*this.getLocalGroupId()*/) {
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

    // getLocalBehaviorEditLink: function() {
    //   if (this.isImporting()) {
    //     return (
    //       <span className="mhm fade-in type-weak type-bold">Installing…</span>
    //     );
    //   }
    //   var localGroupId = this.getLocalGroupId();
    //   if (localGroupId) {
    //     var url = jsRoutes.controllers.BehaviorEditorController.edit(localGroupId).url;
    //     return (
    //       <a
    //         className="mhm fade-in"
    //         href={url}
    //       >Edit installed version</a>
    //     );
    //   } else {
    //     return null;
    //   }
    // },
    //
    // getLocalGroupId: function() {
    //   return this.props.groupData.localGroupId;
    // },

    render: function() {
      return (
        <form ref="form" action={jsRoutes.controllers.BehaviorImportExportController.doImport().url} method="POST">
          <input type="hidden" name="csrfToken" value={this.props.csrfToken} />
          <input type="hidden" name="teamId" value={this.props.teamId} />
          <input type="hidden" name="dataJson" value={JSON.stringify(this.props.groupData)} />
          <div className={"columns columns-elastic mbm " + (this.isImporting() ? "pulse" : "")}>
              <div className="ptxl mobile-pts">
                <h4 className="border-bottom mtm pbm">
                  <span className="phl">{this.getInstallButton()}</span>
                  <span className="type-l">{this.props.name}</span>
                  <span className="type-regular type-weak"> &nbsp; – &nbsp; {this.props.description}</span>
                  {this.getGithubLink()}
                  {/*{this.getLocalBehaviorEditLink()}*/}
                  {this.behaviorAlreadyImported() ? /* TODO: update/re-install buttons */
                    "" : ""}
                </h4>
                {this.getBehaviors().map(function(behavior, index) {
                  return (
                    <Behavior
                      key={"behavior" + index}
                      behaviorData={behavior}
                      csrfToken={this.props.csrfToken}
                      triggers={behavior.triggers}
                      onBehaviorGroupImport={this.props.onBehaviorGroupImport}
                    />
                  );
                }, this)}
              </div>
            </div>
          </form>
      );
    }
  });
});
