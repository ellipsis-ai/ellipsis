define(function(require) {
  var
    React = require('react'),
    SVGInstall = require('../svg/install'),
    SVGInstalled = require('../svg/installed'),
    SVGInstalling = require('../svg/installing'),
    ifPresent = require('../if_present');

  return React.createClass({
    displayName: 'BehaviorGroupCard',
    propTypes: {
      groupData: React.PropTypes.object.isRequired,
      localId: React.PropTypes.string,
      teamId: React.PropTypes.string.isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      description: React.PropTypes.string,
      name: React.PropTypes.string.isRequired,
      icon: React.PropTypes.string,
      onBehaviorGroupImport: React.PropTypes.func.isRequired,
      onMoreInfoClick: React.PropTypes.func.isRequired
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

    getMoreInfoLink: function() {
      if (this.isImporting()) {
        return (
          <span className="fade-in type-weak">Installing…</span>
        );
      } else if (this.props.localId) {
        var url = jsRoutes.controllers.BehaviorEditorController.editGroup(this.props.localId).url;
        return (
          <a
            className="fade-in"
            href={url}
          >View installed version</a>
        );
      } else {
        return (
          <button type="button" className="button-raw button-s" onClick={this.toggleMoreInfo}>More info</button>
        );
      }
    },

    toggleMoreInfo: function() {
      this.props.onMoreInfoClick(this.props.groupData);
    },

    getInstallButton: function() {
      if (this.isImporting()) {
        return (
          <button title="Installing, please wait…" type="button" className="button-raw mbxs" disabled="disabled">
            <span className="display-inline-block align-m mrs" style={{ width: 40, height: 24 }}><SVGInstalling /></span>
            <span className="display-inline-block align-m">
              Installing…
            </span>
          </button>
        );
      } else if (this.isImported()) {
        return (
          <button title="Already installed" type="button" className="button-raw mbxs" disabled="disabled" style={{ height: 24 }}>
            <span className="display-inline-block align-m mrs" style={{ width: 40, height: 24 }}><SVGInstalled /></span>
            <span className="display-inline-block align-m type-green">
              Installed
            </span>
          </button>
        );
      } else {
        return (
          <button title="Install this skill" type="button" className="button-raw mbxs" onClick={this.importBehavior} style={{ height: 24 }}>
            <span className="display-inline-block align-m mrs" style={{ width: 40, height: 24 }}><SVGInstall /></span>
            <span className="display-inline-block align-m">
              Install
            </span>
          </button>
        );
      }
    },

    render: function() {
      return (
        <div className="border border-radius bg-lightest phxl pvl">
          <form ref="form" action={jsRoutes.controllers.BehaviorImportExportController.doImport().url} method="POST">
            <input type="hidden" name="csrfToken" value={this.props.csrfToken} />
            <input type="hidden" name="teamId" value={this.props.teamId} />
            <input type="hidden" name="dataJson" value={JSON.stringify(this.props.groupData)} />
            <div className={this.isImporting() ? "pulse" : ""}>
              <div className="type-l display-ellipsis mbm">
                {ifPresent(this.props.icon, (icon) => (
                  <span style={{ width: "1em" }} className="display-inline-block mrm">
                    {icon}
                  </span>
                ))}
                {this.props.name}
              </div>
              <div className="mvm">{this.getInstallButton()}</div>
              <p className="type-s mvm" style={{ height: "4rem", overflow: "hidden" }}>
                {this.props.description}
              </p>
              <div className="type-s">
                {this.getMoreInfoLink()}
              </div>
            </div>
          </form>
        </div>
      );
    }
  });
});
