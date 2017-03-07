define(function(require) {
  var React = require('react'),
    BehaviorName = require('../behavior_list/behavior_name'),
    Formatter = require('../lib/formatter'),
    SVGInstall = require('../svg/install'),
    SVGInstalled = require('../svg/installed'),
    ifPresent = require('../lib/if_present'),
    Sort = require('../lib/sort');

  return React.createClass({
    displayName: 'BehaviorGroupInfoPanel',
    propTypes: {
      groupData: React.PropTypes.object,
      onBehaviorGroupImport: React.PropTypes.func,
      onToggle: React.PropTypes.func.isRequired,
      isImportable: React.PropTypes.bool.isRequired,
      isImported: React.PropTypes.bool.isRequired
    },

    getBehaviors: function() {
      var behaviorVersions = this.props.groupData && this.props.groupData.behaviorVersions || [];
      return Sort.arrayAlphabeticalBy(behaviorVersions.filter((version) => !version.isDataType()), (version) => version.sortKey);
    },

    getName: function() {
      return this.props.groupData.name || (
          <span className="type-italic type-disabled">Untitled skill</span>
        );
    },

    onImport: function() {
      this.props.onBehaviorGroupImport(this.props.groupData);
      this.props.onToggle();
    },

    toggle: function() {
      this.props.onToggle();
    },

    render: function() {
      if (!this.props.groupData) {
        return null;
      }
      return (
        <div className="box-action phn">
          <div className="container container-c">
            <div className="columns">
              <div className="column column-page-sidebar">
                <h3 className="mtn">
                  {ifPresent(this.props.groupData.icon, (icon) => (
                    <span className="mrm">{icon}</span>
                  ))}
                  <span>{this.getName()}</span>
                </h3>

                {this.renderMetaInfo()}
                {this.renderLastModified()}
              </div>
              <div className="column column-page-main">
                {ifPresent(this.props.groupData.description, (desc) => (
                  <p className="mbl">{desc}</p>
                ))}

                {this.renderBehaviors()}
                <div className="mvxl">
                  {this.renderInstallButton()}
                  <button type="button" className="mrs mbs" onClick={this.toggle}>Done</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    },

    renderBehaviors: function() {
      var behaviors = this.getBehaviors();
      var behaviorCount = behaviors.length;
      return (
        <div className="type-s">
          <h5 className="mbxs">{behaviorCount === 1 ? "1 action" : `${behaviorCount} actions`}</h5>
          <div style={{ overflowY: "auto", maxHeight: "21em" }}>
            {behaviors.map((behavior, index) => (
              <div className="pvs" key={`group-${this.props.groupData.exportId}-behavior${index}`}>
                <BehaviorName
                  version={behavior}
                  disableLink={!behavior.behaviorId}
                  isImportable={true}
                />
              </div>
            ))}
          </div>
        </div>
      );
    },

    renderInstallButton: function() {
      if (this.props.isImportable && !this.props.isImported) {
        return (
          <button type="button" className="button-primary mrs mbs" onClick={this.onImport}>
            <span className="display-inline-block align-b mrm pbxs"
              style={{ width: 25, height: 18 }}><SVGInstall /></span>
            <span className="display-inline-block align-b">Install</span>
          </button>
        );
      }
    },

    renderMetaInfo: function() {
      if (this.props.groupData.githubUrl) {
        return (
          <div className="type-s mvm">
            <a target="_blank" href={this.props.groupData.githubUrl}>
              View source on Github
            </a>
          </div>
        );
      } else if (this.props.isImported) {
        return (
          <div className="type-s mvm">
            <span className="display-inline-block align-m mrs" style={{ width: 30, height: 18 }}><SVGInstalled /></span>
            <span className="display-inline-block align-m type-green">Installed from Ellipsis.ai</span>
          </div>
        );
      }
    },

    renderLastModified: function() {
      if (this.props.groupData.createdAt) {
        return (
          <div className="type-weak type-s mvm">
            Last modified {Formatter.formatTimestampRelativeIfRecent(this.props.groupData.createdAt)}
          </div>
        );
      }
    }
  });
});
