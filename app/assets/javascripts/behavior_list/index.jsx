define(function(require) {
  var React = require('react'),
    BehaviorName = require('./behavior_name'),
    BehaviorGroup = require('../models/behavior_group'),
    BehaviorGroupCard = require('./behavior_group_card'),
    BehaviorGroupInfoPanel = require('./behavior_group_info_panel'),
    Collapsible = require('../shared_ui/collapsible'),
    ConfirmActionPanel = require('../panels/confirm_action'),
    FixedFooter = require('../shared_ui/fixed_footer'),
    Formatter = require('../lib/formatter'),
    ModalScrim = require('../shared_ui/modal_scrim'),
    PageWithPanels = require('../shared_ui/page_with_panels'),
    Sort = require('../lib/sort'),
    SVGInstalled = require('../svg/installed');

  const ANIMATION_DURATION = 0.25;

  const BehaviorList = React.createClass({
    displayName: "BehaviorList",
    propTypes: Object.assign(PageWithPanels.requiredPropTypes(), {
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      csrfToken: React.PropTypes.string.isRequired
    }),

    isGroupImported: function(group) {
      return !!(group && group.exportId);
    },

    getImportedStatusFromGroupOrVersion: function(groupOrVersion) {
      if (groupOrVersion.exportId) {
        return (
          <span title="Installed from ellipsis.ai" className="mls display-inline-block align-m" style={{ width: 30, height: 18 }}>
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
        selectedBehaviorGroup: null,
        selectedGroupIds: [],
        isSubmitting: false,
        footerHeight: 0
      };
    },

    resetFooterHeight: function() {
      var footerHeight = this.refs.footer.getHeight();
      if (this.state.footerHeight !== footerHeight) {
        this.setState({ footerHeight: footerHeight });
      }
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

    confirmDeleteBehaviorGroups: function() {
      this.props.onToggleActivePanel('confirmDeleteBehaviorGroups', true);
    },

    confirmMergeBehaviorGroups: function() {
      this.props.onToggleActivePanel('confirmMergeBehaviorGroups', true);
    },

    onGroupSelectionCheckboxChange: function(groupId, isChecked) {
      var newGroupIds = this.getSelectedGroupIds().slice();
      var index = newGroupIds.indexOf(groupId);
      if (isChecked) {
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
      this.setState({
        isSubmitting: true
      }, () => {
        var url = jsRoutes.controllers.ApplicationController.mergeBehaviorGroups().url;
        this.runSelectedBehaviorGroupsAction(url);
      });
    },

    deleteBehaviorGroups: function() {
      this.setState({
        isSubmitting: true
      }, () => {
        var url = jsRoutes.controllers.ApplicationController.deleteBehaviorGroups().url;
        this.runSelectedBehaviorGroupsAction(url);
      });
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

    getTextForDeleteBehaviorGroups: function(selectedCount) {
      if (selectedCount === 1) {
        return "Are you sure you want to delete this skill?";
      } else {
        return `Are you sure you want to delete these ${selectedCount} skills?`;
      }
    },

    getTextForMergeBehaviorGroups: function(selectedCount) {
      return `Are you sure you want to merge these ${selectedCount} skills?`;
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
          <div className={"column column-expand type-s type-wrap-words " + borderAndSpacingClass}>
            <div className="columns columns-elastic">
              <div className="column column-shrink prs">
                {isFirstRow ? this.renderGroupSelectionCheckbox(group.id) : this.renderPlaceholderCheckbox()}
              </div>
              <div className="column column-expand">
                <BehaviorName
                  version={version}
                  labelDataType={true}
                />
              </div>
            </div>
          </div>
          <div className={"column column-shrink type-s type-weak display-ellipsis align-r mobile-display-none " + borderAndSpacingClass}>
            {Formatter.formatTimestampRelativeIfRecent(version.createdAt)}
            {this.getImportedStatusFromGroupOrVersion(version)}
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
            <div className="column column-expand border-top pts pbxs">
              <div className="columns columns-elastic">
                <div className="column column-shrink prs">{this.renderGroupSelectionCheckbox(group.id)}</div>
                <div className="column column-expand">{this.renderBehaviorGroupTitle(group.name, group.description)}</div>
              </div>
            </div>
            <div className="column column-shrink border-top pts pbxs align-r mobile-display-none">
              {this.getImportedStatusFromGroupOrVersion(group)}
            </div>
          </div>
        );
      } else {
        return null;
      }
    },

    renderBehaviorGroup: function(group) {
      var sorted = Sort.arrayAlphabeticalBy(group.behaviorVersions, (version) => version.sortKey);
      var versionRows = sorted.map((ea, i) => {
        return this.renderBehaviorVersionRow(ea, i, group);
      });
      return [this.renderBehaviorGroupTitleRow(group)].concat(versionRows);
    },

    getSelectedBehaviorGroup: function() {
      return this.state.selectedBehaviorGroup;
    },

    toggleInfoPanel: function(group) {
      var previousSelectedGroup = this.state.selectedBehaviorGroup;
      if (group === previousSelectedGroup && this.props.activePanelName === 'moreInfo') {
        this.props.onClearActivePanel();
        return;
      }

      if (group && group !== previousSelectedGroup) {
        var openNewGroup = () => {
          this.setState({
            selectedBehaviorGroup: group
          }, () => {
            this.props.onToggleActivePanel('moreInfo');
          });
        };
        if (this.props.activePanelName === 'moreInfo') {
          this.props.onClearActivePanel();
          window.setTimeout(openNewGroup, ANIMATION_DURATION * 1000);
        } else {
          openNewGroup();
        }
      } else {
        this.props.onToggleActivePanel('moreInfo');
      }
    },

    renderBehaviorGroups: function() {
      var groups = this.getBehaviorGroups();
      if (groups.length > 0) {
        return groups.map((group, index) => (
          <div className="column column-one-third narrow-column-one-half mobile-column-full phl pbxxl mobile-phn"
            key={"group" + index}>
            <BehaviorGroupCard
              name={group.name}
              description={group.description}
              icon={group.icon}
              groupData={group}
              localId={group.id}
              onMoreInfoClick={this.toggleInfoPanel}
              isImportable={false}
              isImported={this.isGroupImported(group)}
              onSelectChange={this.onGroupSelectionCheckboxChange}
              isSelected={this.isGroupSelected(group.id)}
            />
          </div>
        ));
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
            onClick={this.confirmDeleteBehaviorGroups}
            disabled={selectedCount < 1}
          >
            {this.getLabelForDeleteAction(selectedCount)}
          </button>
          <button type="button"
            className="mrl mbs"
            onClick={this.confirmMergeBehaviorGroups}
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
            <p className="mhl mbxl"><i><b>Tip:</b> mention Ellipsis in chat by starting a message with “…”</i></p>

            <div className="columns">
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
          <div style={{ paddingBottom: `${this.state.footerHeight}px` }}>
            <div className="bg-white container container-c ptxxl mobile-ptm phn">
              {this.renderContent()}
            </div>
          </div>

          <ModalScrim isActive={this.props.activePanelIsModal} onClick={this.props.onClearActivePanel} />
          <FixedFooter ref="footer" className="bg-white">
            <Collapsible
              ref="moreInfo"
              revealWhen={this.props.activePanelName === 'moreInfo'}
              animationDuration={ANIMATION_DURATION}
              onChange={this.resetFooterHeight}
            >
              <BehaviorGroupInfoPanel
                groupData={this.getSelectedBehaviorGroup()}
                onToggle={this.toggleInfoPanel}
                isImportable={false}
                isImported={this.isGroupImported(this.getSelectedBehaviorGroup())}
              />
            </Collapsible>
            <Collapsible
              revealWhen={!this.props.activePanelName && this.getSelectedGroupIds().length > 0}
              onChange={this.resetFooterHeight}
            >
              <div className="container container-c ptm border-top">
                {this.renderActions()}
              </div>
            </Collapsible>
            <Collapsible ref="confirmDeleteBehaviorGroups"
              revealWhen={this.props.activePanelName === 'confirmDeleteBehaviorGroups'}
              onChange={this.resetFooterHeight}
            >
              <ConfirmActionPanel
                confirmText="Delete"
                confirmingText="Deleting"
                onConfirmClick={this.deleteBehaviorGroups}
                onCancelClick={this.props.onClearActivePanel}
                isConfirming={this.state.isSubmitting}
              >
                <p>{this.getTextForDeleteBehaviorGroups(this.getSelectedGroupIds().length)}</p>
              </ConfirmActionPanel>
            </Collapsible>
            <Collapsible ref="confirmMergeBehaviorGroups"
              revealWhen={this.props.activePanelName === 'confirmMergeBehaviorGroups'}
              onChange={this.resetFooterHeight}
            >
              <ConfirmActionPanel
                confirmText="Merge"
                confirmingText="Merging"
                onConfirmClick={this.mergeBehaviorGroups}
                onCancelClick={this.props.onClearActivePanel}
                isConfirming={this.state.isSubmitting}
              >
                <p>{this.getTextForMergeBehaviorGroups(this.getSelectedGroupIds().length)}</p>
              </ConfirmActionPanel>
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  });

  return PageWithPanels.with(BehaviorList);
});
