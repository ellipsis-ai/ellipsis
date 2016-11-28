define(function(require) {
  var React = require('react'),
    BehaviorName = require('./behavior_name'),
    BehaviorVersion = require('../models/behavior_version'),
    Collapsible = require('../collapsible'),
    FixedFooter = require('../fixed_footer'),
    Formatter = require('../formatter'),
    Sort = require('../sort'),
    SVGInstalled = require('../svg/installed');

  return React.createClass({
    displayName: "BehaviorList",
    propTypes: {
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.shape({
        id: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        createdAt: React.PropTypes.number.isRequired
      })).isRequired,
      behaviorVersions: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorVersion)).isRequired,
      csrfToken: React.PropTypes.string.isRequired
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
      return this.props.behaviorVersions;
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
      groups = groups.map(ea => {
        return { id: ea.id, versions: this.sortVersions(ea.versions) };
      });
      return Sort.arrayAlphabeticalBy(groups, group => {
        return group.versions[0].getFirstTriggerText();
      });
    },

    sortVersionsByDataTypeName: function(versions) {
      return Sort.arrayAlphabeticalBy(versions, (item) => item.getDataTypeName());
    },

    sortVersionsByFirstTrigger: function(versions) {
      return Sort.arrayAlphabeticalBy(versions, (item) => item.getFirstTriggerText());
    },

    sortVersions: function(versions) {
      var actionVersions = versions.filter(ea => !ea.isDataType());
      var dataTypeVersions = versions.filter(ea => ea.isDataType());
      return this.sortVersionsByFirstTrigger(actionVersions).concat(this.sortVersionsByDataTypeName(dataTypeVersions));
    },

    getInitialState: function() {
      return {
        versions: this.getVersions(),
        selectedGroupIds: []
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

    getSelectedGroupIds: function() {
      return this.state.selectedGroupIds || [];
    },

    isGroupSelected: function(groupId) {
      return this.getSelectedGroupIds().indexOf(groupId) >= 0;
    },

    onGroupSelectionCheckboxChange: function(groupId) {
      return function(event) {
        var newGroupIds = this.getSelectedGroupIds().slice();
        var index = newGroupIds.indexOf(groupId);
        if (event.target.checked) {
          if (index === -1) {
            newGroupIds.push(groupId);
          }
        } else {
          if (index >= 0) {
            newGroupIds.splice(index, 1);
          }
        }
        this.setState({
          selectedGroupIds: newGroupIds
        });
      }.bind(this);
    },

    clearSelectedGroups: function() {
      this.setState({
        selectedGroupIds: []
      });
    },

    runSelectedBehaviorGroupsAction: function(url) {
      var data = {
        behaviorGroupIds: this.getSelectedGroupIds()
      };
      fetch(url, {
        credentials: 'same-origin',
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Csrf-Token': this.props.csrfToken
        },
        body: JSON.stringify(data)
      }).then(() => {
        window.location.reload();
      });
    },

    mergeBehaviorGroups: function() {
      var url = jsRoutes.controllers.ApplicationController.mergeBehaviorGroups().url;
      this.runSelectedBehaviorGroupsAction(url);
    },

    deleteBehaviorGroups: function() {
      var url = jsRoutes.controllers.ApplicationController.deleteBehaviorGroups().url;
      this.runSelectedBehaviorGroupsAction(url);
    },

    renderGroupSelectionCheckbox: function(groupId) {
      return (
        <input
          type="checkbox"
          onChange={this.onGroupSelectionCheckboxChange(groupId)}
          ref={groupId}
          key={groupId}
          checked={this.isGroupSelected(groupId)}
        />
      );
    },

    getActionsLabel: function(selectedCount) {
      if (selectedCount === 0) {
        return "No skills selected";
      } else if (selectedCount === 1) {
        return "1 skill selected";
      } else {
        return `${selectedCount} skills selected`;
      }
    },

    getLabelForDeleteAction: function(selectedCount) {
      if (selectedCount < 2) {
        return "Delete skill";
      } else {
        return `Delete skills`;
      }
    },

    getVersionRow: function(version, versionIndex, group) {
      var isFirstVersion = versionIndex === 0;
      var borderAndSpacingClass = isFirstVersion ? "border-top pts " : "";
      borderAndSpacingClass += (versionIndex === group.versions.length - 1 ? "pbs " : "pbxs ");
      return (
        <div className="column-row" key={`version-${group.id}-${versionIndex}`}>
          <div className={"column column-shrink type-s type-weak display-ellipsis align-r mobile-display-none " + borderAndSpacingClass}>
            {isFirstVersion ? this.renderGroupSelectionCheckbox(group.id) : ""}
          </div>
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
              <div className="column column-shrink pbs">&nbsp;</div>
              <div className="column column-expand type-label align-b pbs">Skills</div>
              <div className="column column-shrink type-label align-r pbs align-b mobile-display-none">Last modified</div>
            </div>
            {groups.map(this.getBehaviorGroupRow)}
          </div>
        );
      }
    },

    renderActions: function() {
      var selectedCount = this.getSelectedGroupIds().length;
      return (
        <div>
          <button type="button"
            className="button-primary mrs mbs"
            onClick={this.clearSelectedGroups}
          >
            Cancel
          </button>
          <button type="button"
            className="mrs mbs"
            onClick={this.deleteBehaviorGroups}
            disabled={selectedCount < 1}
          >
            {this.getLabelForDeleteAction(selectedCount)}
          </button>
          <button type="button"
            className="mrl mbs"
            onClick={this.mergeBehaviorGroups}
            disabled={selectedCount < 2}
          >
            Merge skills
          </button>
          <div className="align-button mrs mbs type-italic type-weak">
            {this.getActionsLabel(selectedCount)}
          </div>
        </div>
      );
    },

    renderContent: function() {
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
    },

    render: function() {
      return (
        <div>
          <div>
            <div className="bg-white container pvxxl mobile-ptm">
              {this.renderContent()}
            </div>
          </div>

          <FixedFooter className="bg-white">
            <Collapsible revealWhen={this.getSelectedGroupIds().length > 0}>
              <div className="container ptm">
                {this.renderActions()}
              </div>
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  });
});
