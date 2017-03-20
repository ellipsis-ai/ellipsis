define(function(require) {
  var React = require('react'),
    BehaviorGroup = require('../models/behavior_group'),
    BehaviorGroupCard = require('./behavior_group_card'),
    BehaviorGroupInfoPanel = require('./behavior_group_info_panel'),
    Collapsible = require('../shared_ui/collapsible'),
    ConfirmActionPanel = require('../panels/confirm_action'),
    FixedFooter = require('../shared_ui/fixed_footer'),
    InstalledBehaviorGroupsPanel = require('./installed_behavior_groups_panel'),
    ModalScrim = require('../shared_ui/modal_scrim'),
    PageWithPanels = require('../shared_ui/page_with_panels');

  const ANIMATION_DURATION = 0.25;

  const BehaviorList = React.createClass({
    displayName: "BehaviorList",
    propTypes: Object.assign(PageWithPanels.requiredPropTypes(), {
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      onLoadPublishedBehaviorGroups: React.PropTypes.func.isRequired,
      onBehaviorGroupImport: React.PropTypes.func.isRequired,
      publishedBehaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)),
      publishedBehaviorGroupLoadStatus: React.PropTypes.string.isRequired,
      recentlyInstalled: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)),
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      slackTeamId: React.PropTypes.string.isRequired
    }),

    getInitialState: function() {
      return {
        selectedBehaviorGroup: null,
        selectedGroupIds: [],
        isSubmitting: false,
        footerHeight: 0,
        importingList: []
      };
    },

    componentWillReceiveProps: function(nextProps) {
      var updatedImportingList = this.state.importingList.filter((importing) =>
        !BehaviorGroup.groupsIncludeExportId(nextProps.recentlyInstalled, importing.exportId)
      );
      this.setState({
        importingList: updatedImportingList
      });
      var hasNewRecentlyInstalled = nextProps.recentlyInstalled.some((nextInstalled) =>
        !BehaviorGroup.groupsIncludeId(this.props.recentlyInstalled, nextInstalled.id));
      if (hasNewRecentlyInstalled && this.props.activePanelName !== 'afterInstall') {
        this.props.onToggleActivePanel('afterInstall');
      }
    },

    getAnimationDuration: function() {
      return ANIMATION_DURATION;
    },

    getBehaviorGroups: function() {
      return this.props.behaviorGroups.concat(this.props.recentlyInstalled);
    },

    getBehaviorGroupsJustInstalled: function() {
      return this.props.recentlyInstalled;
    },

    resetFooterHeight: function() {
      var footerHeight = this.refs.footer.getHeight();
      if (this.state.footerHeight !== footerHeight) {
        this.setState({ footerHeight: footerHeight });
      }
    },

    getSelectedGroupIds: function() {
      return this.state.selectedGroupIds || [];
    },

    getLocalIdFor: function(exportId) {
      var localGroup = this.getBehaviorGroups().find((ea) => ea.exportId === exportId);
      return localGroup ? localGroup.id : null;
    },

    isGroupSelected: function(groupId) {
      return this.getSelectedGroupIds().indexOf(groupId) >= 0;
    },

    confirmDeleteBehaviorGroups: function() {
      this.toggleActivePanel('confirmDeleteBehaviorGroups', true);
    },

    confirmMergeBehaviorGroups: function() {
      this.toggleActivePanel('confirmMergeBehaviorGroups', true);
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

    getSelectedBehaviorGroup: function() {
      return this.state.selectedBehaviorGroup;
    },

    selectedBehaviorGroupIsImportable: function() {
      var selectedGroup = this.getSelectedBehaviorGroup();
      return !!(selectedGroup && selectedGroup.exportId && !this.getLocalIdFor(selectedGroup));
    },

    getSelectedBehaviorGroupId: function() {
      var group = this.getSelectedBehaviorGroup();
      return group ? group.id : null;
    },

    hasRecentlyInstalledBehaviorGroups: function() {
      return this.getBehaviorGroupsJustInstalled().length > 0;
    },

    getActivePanelName: function() {
      return this.props.activePanelName;
    },

    clearActivePanel: function() {
      this.props.onClearActivePanel();
    },

    toggleActivePanel: function(panelName, beModal) {
      this.props.onToggleActivePanel(panelName, beModal);
    },

    getUninstalledBehaviorGroups: function() {
      return this.props.publishedBehaviorGroups.filter((published) =>
        !BehaviorGroup.groupsIncludeExportId(this.props.behaviorGroups, published.exportId)
      );
    },

    onBehaviorGroupImport: function(groupToInstall) {
      this.setState({
        importingList: this.state.importingList.concat([groupToInstall])
      }, () => {
        if (this.getActivePanelName() === 'moreInfo') {
          this.clearActivePanel();
        }
        this.props.onBehaviorGroupImport(groupToInstall);
      });
    },

    isImporting: function(group) {
      return BehaviorGroup.groupsIncludeExportId(this.state.importingList, group.exportId);
    },

    toggleInfoPanel: function(group) {
      var previousSelectedGroup = this.state.selectedBehaviorGroup;
      var panelOpen = this.getActivePanelName() === 'moreInfo';

      if (panelOpen) {
        this.clearActivePanel();
      }

      if (group && group === previousSelectedGroup && !panelOpen) {
        this.toggleActivePanel('moreInfo');
      } else if (group && group !== previousSelectedGroup) {
        var openNewGroup = () => {
          this.setState({
            selectedBehaviorGroup: group
          }, () => {
            this.toggleActivePanel('moreInfo');
          });
        };
        if (panelOpen) {
          setTimeout(openNewGroup, this.getAnimationDuration() * 1000);
        } else {
          openNewGroup();
        }
      }
    },

    renderTeachButton: function() {
      return (
        <a href={jsRoutes.controllers.BehaviorEditorController.newGroup(this.props.teamId).url}
          className="button button-shrink">
          Teach Ellipsis something new…
        </a>
      );
    },

    renderInstalledBehaviorGroups: function(groups) {
      return (
        <div className="container container-c ptl pbxl mobile-ptm phn">

          <div className="columns columns-elastic mobile-columns-float">
            <div className="column column-expand">
              <h3 className="type-blue-faded mbxl mhl mobile-mbm">Your skills</h3>
            </div>
            <div className="column column-shrink align-m phl mobile-pbl">
              {this.renderTeachButton()}
            </div>
          </div>

          <div className="columns">
            {groups.map((group, index) => (
              <div className="column column-one-third narrow-column-one-half mobile-column-full phl pbxxl mobile-pbl"
                key={"group" + index}>
                <BehaviorGroupCard
                  name={group.name}
                  description={group.description}
                  icon={group.icon}
                  groupData={group}
                  localId={group.id}
                  onMoreInfoClick={this.toggleInfoPanel}
                  isImportable={false}
                  onSelectChange={this.onGroupSelectionCheckboxChange}
                  isSelected={this.isGroupSelected(group.id)}
                  cardClassName="bg-white"
                />
              </div>
            ))}
          </div>
        </div>
      );
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

    renderPublishedGroups: function() {
      return (
        <div className="bg-blue-lighter pbxl">
          <hr className="mtn bg-dark-translucent" />
          <div className="container container-c phn">
            {this.renderPublishedGroupsContent()}
          </div>
        </div>
      );
    },

    renderPublishedGroupsContent: function() {
      var groups = this.getUninstalledBehaviorGroups();
      if (this.props.publishedBehaviorGroupLoadStatus === 'loaded' && groups.length > 1) {
        return (
          <div>

            <h3 className="mtxxxl mbxl mhl type-blue-faded">Skills published by Ellipsis.ai (available to install)</h3>

            <div className="columns">
              {groups.map((group, index) => (
                <div
                  className="column column-one-third narrow-column-one-half mobile-column-full phl pbxxl mobile-pbl"
                  key={"group" + index}>
                  <BehaviorGroupCard
                    name={group.name}
                    description={group.description}
                    icon={group.icon}
                    groupData={group}
                    localId={this.getLocalIdFor(group.exportId)}
                    onBehaviorGroupImport={this.onBehaviorGroupImport}
                    onMoreInfoClick={this.toggleInfoPanel}
                    isImporting={this.isImporting(group)}
                    isImportable={true}
                    cardClassName="bg-blue-lightest"
                  />
                </div>
              ))}
            </div>
          </div>
        );
      } else if (this.props.publishedBehaviorGroupLoadStatus === 'loading') {
        return (
          <div className="pulse phl">
            <p>
              <i>Loading skills published by Ellipsis.ai…</i>
            </p>
          </div>
        );
      } else if (this.props.publishedBehaviorGroupLoadStatus === 'error') {
        return (
          <div className="phl">
            <p>
              An error occurred loading the list of published skills.
            </p>

            <button type="button" onClick={this.props.onLoadPublishedBehaviorGroups}>Try again…</button>
          </div>
        );
      }
    },

    renderContent: function() {
      var localGroups = this.getBehaviorGroups();
      if (localGroups.length > 0) {
        return (
          <div>

            {this.renderInstalledBehaviorGroups(localGroups)}

            {this.renderPublishedGroups()}

          </div>
        );
      } else {
        return (
          <div>
            <p className="type-l pvxl">
              Ellipsis doesn’t know any skills yet. Try installing some of the ones
              published by Ellipsis, or create a new one yourself.
            </p>

            {this.renderTeachButton()}

            {this.renderPublishedGroups()}

          </div>
        );
      }
    },

    render: function() {
      return (
        <div>
          <div style={{ paddingBottom: `${this.state.footerHeight}px` }}>
            <div>
              {this.renderContent()}
            </div>
          </div>

          <ModalScrim isActive={this.props.activePanelIsModal} onClick={this.clearActivePanel}/>
          <FixedFooter ref="footer" className="bg-white">
            <Collapsible
              ref="moreInfo"
              revealWhen={this.getActivePanelName() === 'moreInfo'}
              animationDuration={this.getAnimationDuration()}
              onChange={this.resetFooterHeight}
            >
              <BehaviorGroupInfoPanel
                groupData={this.getSelectedBehaviorGroup()}
                onToggle={this.clearActivePanel}
                isImportable={this.selectedBehaviorGroupIsImportable()}
                localId={this.getSelectedBehaviorGroupId()}
                onBehaviorGroupImport={this.onBehaviorGroupImport}
              />
            </Collapsible>
            <Collapsible
              ref="afterInstall"
              revealWhen={this.hasRecentlyInstalledBehaviorGroups() && this.getActivePanelName() === 'afterInstall'}
              animationDuration={this.getAnimationDuration()}
              onChange={this.resetFooterHeight}
            >
              <InstalledBehaviorGroupsPanel
                installedBehaviorGroups={this.getBehaviorGroupsJustInstalled()}
                onToggle={this.props.onClearActivePanel}
                slackTeamId={this.props.slackTeamId}
              />
            </Collapsible>
            <Collapsible
              revealWhen={!this.props.activePanelIsModal && this.getSelectedGroupIds().length > 0}
              onChange={this.resetFooterHeight}
            >
              <div className="border-top">
                <div className="container container-c ptm">
                  {this.renderActions()}
                </div>
              </div>
            </Collapsible>
            <Collapsible ref="confirmDeleteBehaviorGroups"
              revealWhen={this.getActivePanelName() === 'confirmDeleteBehaviorGroups'}
              onChange={this.resetFooterHeight}
            >
              <ConfirmActionPanel
                confirmText="Delete"
                confirmingText="Deleting"
                onConfirmClick={this.deleteBehaviorGroups}
                onCancelClick={this.clearActivePanel}
                isConfirming={this.state.isSubmitting}
              >
                <p>{this.getTextForDeleteBehaviorGroups(this.getSelectedGroupIds().length)}</p>
              </ConfirmActionPanel>
            </Collapsible>
            <Collapsible ref="confirmMergeBehaviorGroups"
              revealWhen={this.getActivePanelName() === 'confirmMergeBehaviorGroups'}
              onChange={this.resetFooterHeight}
            >
              <ConfirmActionPanel
                confirmText="Merge"
                confirmingText="Merging"
                onConfirmClick={this.mergeBehaviorGroups}
                onCancelClick={this.clearActivePanel}
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
