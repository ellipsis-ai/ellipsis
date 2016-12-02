define(function(require) {
  var React = require('react'),
    BehaviorName = require('./behavior_name'),
    BehaviorGroup = require('../models/behavior_group'),
    Collapsible = require('../collapsible'),
    FixedFooter = require('../fixed_footer'),
    Formatter = require('../formatter'),
    SVGInstalled = require('../svg/installed');

  return React.createClass({
    displayName: "BehaviorList",
    propTypes: {
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
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

    getBehaviorGroups: function() {
      return this.props.behaviorGroups;
    },

    getInitialState: function() {
      return {
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
          className="align-t"
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

    groupHasTitle: function(group) {
      return !!(group.name || group.description);
    },

    renderBehaviorVersionRow: function(version, versionIndex, group) {
      var isFirstRow = versionIndex === 0 && !this.groupHasTitle(group);
      var borderAndSpacingClass = isFirstRow ? "border-top pts " : "";
      borderAndSpacingClass += (versionIndex === group.behaviorVersions.length - 1 ? "pbs " : "pbxs ");
      return (
        <div className="column-row" key={`version-${group.id}-${versionIndex}`}>
          <div className={"column column-shrink type-s type-weak display-ellipsis align-r mobile-display-none " + borderAndSpacingClass}>
            {isFirstRow ? this.renderGroupSelectionCheckbox(group.id) : ""}
          </div>
          <div className={"column column-expand type-s type-wrap-words " + borderAndSpacingClass}>
            <BehaviorName version={version} labelDataType={true} />
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

    renderBehaviorGroupTitle: function(optionalName, optionalDescription) {
      if (optionalName && optionalDescription) {
        return (
          <h4 className="man">
            <span>{optionalName}</span>
            <span className="mhs type-weak">·</span>
            <span className="type-regular">{optionalDescription}</span>
          </h4>
        );
      } else if (optionalName) {
        return (
          <h4 className="man">{optionalName}</h4>
        );
      } else if (optionalDescription) {
        return (
          <h4 className="type-regular man">{optionalDescription}</h4>
        );
      } else {
        return null;
      }
    },

    renderBehaviorGroupTitleRow: function(group) {
      if (this.groupHasTitle(group)) {
        return (
          <div className="column-row" key={`group-${group.id}-title`}>
            <div className="column column-shrink border-top pts pbxs">{this.renderGroupSelectionCheckbox(group.id)}</div>
            <div className="column column-expand border-top pts pbxs">{this.renderBehaviorGroupTitle(group.name, group.description)}</div>
            <div className="column column-shrink border-top pts pbxs"></div>
            <div className="column column-shrink border-top pts pbxs"></div>
          </div>
        );
      } else {
        return null;
      }
    },

    renderBehaviorGroup: function(group) {
      var versionRows = group.behaviorVersions.map((ea, i) => {
        return this.renderBehaviorVersionRow(ea, i, group);
      });
      return [this.renderBehaviorGroupTitleRow(group)].concat(versionRows);
    },

    renderBehaviorGroups: function() {
      var groups = this.getBehaviorGroups();
      if (groups.length > 0) {
        return (
          <div className="column-group">
            <div className="column-row type-bold">
              <div className="column column-shrink pbs">&nbsp;</div>
              <div className="column column-expand type-label align-b pbs">Skills</div>
              <div className="column column-shrink type-label align-r pbs align-b mobile-display-none">Last modified</div>
            </div>
            {groups.map(this.renderBehaviorGroup)}
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
      if (this.props.behaviorGroups.length > 0) {
        return (
          <div>
            <p><i><b>Tip:</b> mention Ellipsis in chat by starting a message with “…”</i></p>

            <div className="columns columns-elastic mobile-columns-float">
              {this.renderBehaviorGroups()}
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
            <div className="bg-white container container-c pvxxl mobile-ptm">
              {this.renderContent()}
            </div>
          </div>

          <FixedFooter className="bg-white">
            <Collapsible revealWhen={this.getSelectedGroupIds().length > 0}>
              <div className="container container-c ptm">
                {this.renderActions()}
              </div>
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  });
});
