define(function(require) {
  var React = require('react'),
    BehaviorName = require('./behavior_name'),
    BehaviorVersion = require('../models/behavior_version'),
    Formatter = require('../formatter'),
    Sort = require('../sort'),
    SVGInstalled = require('../svg/installed');

  return React.createClass({
    propTypes: {
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.shape({
        id: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        createdAt: React.PropTypes.number.isRequired
      })).isRequired,
      behaviorVersions: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired
    },

    getImportedStatusFromVersion: function(version) {
      if (version.importedId) {
        return (
          <span title="Installed from ellipsis.ai" className="mtxs display-inline-block" style={{ width: 30, height: 18 }}>
            <SVGInstalled />
          </span>
        );
      }
    },

    getVersions: function() {
      return this.sortVersionsByFirstTrigger(this.props.behaviorVersions);
    },

    getBehaviorGroups: function() {
      var groups = [];
      this.props.behaviorVersions.forEach(version => {
        var gid = version.groupId;
        var group = groups.find(ea => ea.id === gid);
        if (!group) {
          group = { id: gid, versions: [] };
          groups.push(group);
        }
        group.versions.push(version);
      });
      return Sort.arrayAlphabeticalBy(groups, group => {
        return group.versions[0].getFirstTriggerText();
      });
    },

    sortVersionsByFirstTrigger: function(versions) {
      return Sort.arrayAlphabeticalBy(versions, (item) => item.getFirstTriggerText());
    },

    getInitialState: function() {
      return {
        versions: this.getVersions()
      };
    },

    getTableRowClasses: function(index) {
      if (index % 3 === 0) {
        return " pts border-top ";
      } else if (index % 3 === 2) {
        return " pbs ";
      } else {
        return "";
      }
    },

    getVersionRow: function(version, versionIndex, group) {
      var borderAndSpacingClass = versionIndex === 0 ? "border-top pts " : "";
      borderAndSpacingClass += (versionIndex === group.versions.length - 1 ? "pbs " : "pbxs ");
      return (
        <div className="column-row" key={`version-${group.id}-${versionIndex}`}>
          <div className={"column column-expand type-s type-wrap-words " + borderAndSpacingClass}>
            <BehaviorName version={version} />
          </div>
          <div className={"column column-shrink type-s type-weak display-ellipsis align-r mobile-display-none " + borderAndSpacingClass}>
            {Formatter.formatTimestampRelativeIfRecent(version.createdAt)}
          </div>
          <div className={"column column-shrink mobile-display-none " + borderAndSpacingClass}>
            {this.getImportedStatusFromVersion(version)}
          </div>
        </div>
      );
    },

    getBehaviorGroupRow: function(group) {
      return group.versions.map((ea, i) => {
        return this.getVersionRow(ea, i, group);
      });
    },

    getBehaviorGroupRows: function() {
      var groups = this.getBehaviorGroups();
      if (groups.length > 0) {
        return (
          <div className="column-group">
            <div className="column-row type-bold">
              <div className="column column-expand ptl type-l pbs">What Ellipsis can do</div>
              <div className="column column-shrink type-label align-r pbs align-b mobile-display-none">Last modified</div>
            </div>
            {groups.map(this.getBehaviorGroupRow)}
          </div>
        );
      }
    },

    render: function() {
      if (this.props.behaviorVersions.length > 0) {
        return (
          <div>
            <p><i><b>Tip:</b> mention Ellipsis in chat by starting a message with “…”</i></p>

            <div className="columns columns-elastic mobile-columns-float">
              {this.getBehaviorGroupRows()}
            </div>
          </div>
        );
      } else {
        return (
          <p className="type-l pvxl">
          Ellipsis doesn’t know any skills yet. Try installing some of the ones
          published by Ellipsis, or create a new one yourself.
          </p>
        );
      }
    }
  });
});
