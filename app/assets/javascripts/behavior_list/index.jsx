define(function(require) {
  var React = require('react'),
    EditableName = require('./editable_name'),
    BehaviorGroup = require('../models/behavior_group'),
    BehaviorGroupCard = require('./behavior_group_card'),
    BehaviorGroupInfoPanel = require('./behavior_group_info_panel'),
    Collapsible = require('../shared_ui/collapsible'),
    ConfirmActionPanel = require('../panels/confirm_action'),
    FixedFooter = require('../shared_ui/fixed_footer'),
    SearchInput = require('../form/search'),
    InstalledBehaviorGroupsPanel = require('./installed_behavior_groups_panel'),
    ListHeading = require('./list_heading'),
    ModalScrim = require('../shared_ui/modal_scrim'),
    Page = require('../shared_ui/page'),
    ResponsiveColumn = require('../shared_ui/responsive_column'),
    SubstringHighlighter = require('../shared_ui/substring_highlighter'),
    debounce = require('javascript-debounce');

  const ANIMATION_DURATION = 0.25;

  const BehaviorList = React.createClass({
    propTypes: Object.assign({}, Page.requiredPropTypes, {
      onLoadPublishedBehaviorGroups: React.PropTypes.func.isRequired,
      onBehaviorGroupImport: React.PropTypes.func.isRequired,
      onBehaviorGroupUpdate: React.PropTypes.func.isRequired,
      onMergeBehaviorGroups: React.PropTypes.func.isRequired,
      onDeleteBehaviorGroups: React.PropTypes.func.isRequired,
      onSearch: React.PropTypes.func.isRequired,
      localBehaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      publishedBehaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      recentlyInstalled: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      currentlyInstalling: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      matchingResults: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      currentSearchText: React.PropTypes.string.isRequired,
      isLoadingMatchingResults: React.PropTypes.bool.isRequired,
      publishedBehaviorGroupLoadStatus: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      slackTeamId: React.PropTypes.string.isRequired
    }),

    getDefaultProps: function() {
      return Page.requiredPropDefaults();
    },

    getInitialState: function() {
      return {
        selectedBehaviorGroup: null,
        checkedGroupIds: [],
        isSubmitting: false,
        footerHeight: 0,
        searchText: ""
      };
    },

    componentWillReceiveProps: function(nextProps) {
      const newestImported = nextProps.recentlyInstalled.filter((next) => !BehaviorGroup.groupsIncludeExportId(this.props.recentlyInstalled, next.exportId));
      const newlyInstalled = newestImported.filter((newGroup) => !BehaviorGroup.groupsIncludeExportId(this.props.localBehaviorGroups, newGroup.exportId));
      if (newlyInstalled.length > 0 && this.props.activePanelName !== 'afterInstall') {
        this.props.onToggleActivePanel('afterInstall');
      }
    },

    updateSearch: function(newValue) {
      this.setState({
        searchText: newValue
      }, () => {
        if (newValue) {
          this.delaySubmitSearch();
        } else {
          this.submitSearch();
        }
      });
    },

    submitSearch: function() {
      this.props.onSearch(this.state.searchText);
    },

    delaySubmitSearch: debounce(function() { this.submitSearch(); }, 500),

    getAnimationDuration: function() {
      return ANIMATION_DURATION;
    },

    getLocalBehaviorGroups: function() {
      const newGroups = this.props.recentlyInstalled.slice();
      const localGroups = this.props.localBehaviorGroups.map((group) => {
        const updatedIndex = newGroups.findIndex((newGroup) => newGroup.id === group.id);
        if (updatedIndex >= 0) {
          return newGroups.splice(updatedIndex, 1)[0];
        } else {
          return group;
        }
      });
      return localGroups.concat(newGroups);
    },

    hasLocalBehaviorGroups: function() {
      return this.getLocalBehaviorGroups().length > 0;
    },

    isSearching: function() {
      return this.props.currentSearchText && this.props.currentSearchText.length;
    },

    getMatchingBehaviorGroupsFrom: function(groups) {
      if (this.isSearching()) {
        return groups.filter((ea) =>
          BehaviorGroup.groupsIncludeExportId(this.props.matchingResults, ea.exportId)
        );
      } else {
        return groups;
      }
    },

    getUninstalledBehaviorGroups: function() {
      return this.props.publishedBehaviorGroups.filter((published) =>
        !BehaviorGroup.groupsIncludeExportId(this.props.localBehaviorGroups, published.exportId)
      );
    },

    getLocalBehaviorGroupsJustInstalled: function() {
      return this.props.recentlyInstalled;
    },

    resetFooterHeight: function() {
      var footerHeight = this.refs.footer.getHeight();
      if (this.state.footerHeight !== footerHeight) {
        this.setState({ footerHeight: footerHeight });
      }
    },

    getCheckedGroupIds: function() {
      return this.state.checkedGroupIds || [];
    },

    getLocalIdFor: function(exportId) {
      var localGroup = this.getLocalBehaviorGroups().find((ea) => ea.exportId === exportId);
      return localGroup ? localGroup.id : null;
    },

    isGroupChecked: function(group) {
      return group.id && this.getCheckedGroupIds().indexOf(group.id) >= 0;
    },

    confirmDeleteBehaviorGroups: function() {
      this.toggleActivePanel('confirmDeleteBehaviorGroups', true);
    },

    confirmMergeBehaviorGroups: function() {
      this.toggleActivePanel('confirmMergeBehaviorGroups', true);
    },

    onGroupCheckboxChange: function(groupId, isChecked, optionalCallback) {
      var newGroupIds = this.getCheckedGroupIds().slice();
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
        checkedGroupIds: newGroupIds
      }, optionalCallback);
    },

    clearCheckedGroups: function() {
      this.setState({
        checkedGroupIds: []
      });
    },

    mergeBehaviorGroups: function() {
      this.setState({
        isSubmitting: true
      }, () => {
        this.props.onMergeBehaviorGroups(this.getCheckedGroupIds());
      });
    },

    deleteBehaviorGroups: function() {
      this.setState({
        isSubmitting: true
      }, () => {
        this.props.onDeleteBehaviorGroups(this.getCheckedGroupIds());
      });
    },

    getActionsLabel: function(checkedCount) {
      if (checkedCount === 0) {
        return "No skills selected";
      } else if (checkedCount === 1) {
        return "1 skill selected";
      } else {
        return `${checkedCount} skills selected`;
      }
    },

    getLabelForDeleteAction: function(checkedCount) {
      if (checkedCount < 2) {
        return "Delete skill";
      } else {
        return `Delete skills`;
      }
    },

    getTextForDeleteBehaviorGroups: function(checkedCount) {
      if (checkedCount === 1) {
        return "Are you sure you want to delete this skill?";
      } else {
        return `Are you sure you want to delete these ${checkedCount} skills?`;
      }
    },

    getTextForMergeBehaviorGroups: function(checkedCount) {
      return `Are you sure you want to merge these ${checkedCount} skills?`;
    },

    getSelectedBehaviorGroup: function() {
      return this.state.selectedBehaviorGroup;
    },

    selectedBehaviorGroupIsUninstalled: function() {
      var selectedGroup = this.getSelectedBehaviorGroup();
      return !!(selectedGroup && selectedGroup.exportId && !this.getLocalIdFor(selectedGroup.exportId));
    },

    selectedBehaviorWasImported: function() {
      var selectedGroup = this.getSelectedBehaviorGroup();
      return !!(
        selectedGroup &&
        selectedGroup.id &&
        BehaviorGroup.groupsIncludeExportId(this.props.publishedBehaviorGroups, selectedGroup.exportId)
      );
    },

    getSelectedBehaviorGroupId: function() {
      var group = this.getSelectedBehaviorGroup();
      return group ? group.id : null;
    },

    hasRecentlyInstalledBehaviorGroups: function() {
      return this.getLocalBehaviorGroupsJustInstalled().length > 0;
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

    onBehaviorGroupImport: function(groupToInstall) {
      if (this.getActivePanelName() === 'moreInfo') {
        this.clearActivePanel();
      }
      this.props.onBehaviorGroupImport(groupToInstall);
    },

    onBehaviorGroupUpdate: function(existingGroup, updatedData) {
      if (this.getActivePanelName() === 'moreInfo') {
        this.clearActivePanel();
      }
      const callback = () => {
        this.props.onBehaviorGroupUpdate(existingGroup, updatedData);
      };
      if (this.isGroupChecked(existingGroup)) {
        this.onGroupCheckboxChange(existingGroup.id, false, callback);
      } else {
        callback();
      }
    },

    getUpdatedBehaviorGroupData: function() {
      const selected = this.getSelectedBehaviorGroup();
      if (selected && selected.exportId && selected.id) {
        return this.props.publishedBehaviorGroups.find((ea) => ea.exportId === selected.exportId);
      } else {
        return null;
      }
    },

    isImporting: function(group) {
      return BehaviorGroup.groupsIncludeExportId(this.props.currentlyInstalling, group.exportId);
    },

    wasReimported: function(group) {
      return BehaviorGroup.groupsIncludeExportId(this.props.localBehaviorGroups, group.exportId) &&
        BehaviorGroup.groupsIncludeExportId(this.props.recentlyInstalled, group.exportId);
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

    highlight: function(text) {
      if (text) {
        return (
          <SubstringHighlighter text={text} substring={this.props.currentSearchText}/>
        );
      } else {
        return null;
      }
    },

    getDescriptionOrMatchingTriggers: function(group) {
      var lowercaseDescription = group.getDescription().toLowerCase();
      var lowercaseSearch = this.props.currentSearchText.toLowerCase();
      var matchingBehaviorVersions = [];
      if (lowercaseSearch) {
        matchingBehaviorVersions = group.behaviorVersions.filter((version) => version.includesText(lowercaseSearch));
      }
      if (!lowercaseSearch || lowercaseDescription.includes(lowercaseSearch) || matchingBehaviorVersions.length === 0) {
        return this.highlight(group.description);
      } else {
        return (
          <div>
            {matchingBehaviorVersions.map((version) => (
              <EditableName
                className="mbs"
                version={version}
                disableLink={true}
                key={`matchingBehaviorVersion${version.behaviorId || version.exportId}`}
                highlightText={this.props.currentSearchText}
              />
            ))}
          </div>
        );
      }
    },

    renderInstalledBehaviorGroups: function() {
      var allLocal = this.getLocalBehaviorGroups();
      var groups = this.getMatchingBehaviorGroupsFrom(allLocal);
      return (
        <Collapsible revealWhen={allLocal.length > 0} animationDuration={0.5}>
          <div className="container container-c mvxl">

            <ListHeading teamId={this.props.teamId} includeTeachButton={true}>
              {this.isSearching() ?
                `Your skills matching “${this.props.currentSearchText}”` :
                "Your skills"
              }
            </ListHeading>

            <div className={"columns mvxl " + (this.props.isLoadingMatchingResults ? "pulse-faded" : "")}>
              {groups.length > 0 ? groups.map((group) => (
                <ResponsiveColumn key={group.id}>
                  <BehaviorGroupCard
                    name={this.highlight(group.name)}
                    description={this.getDescriptionOrMatchingTriggers(group)}
                    icon={group.icon}
                    groupData={group}
                    localId={group.id}
                    onMoreInfoClick={this.toggleInfoPanel}
                    isImportable={false}
                    isImporting={this.isImporting(group)}
                    onCheckedChange={this.onGroupCheckboxChange}
                    isChecked={this.isGroupChecked(group)}
                    wasReimported={this.wasReimported(group)}
                    cardClassName="bg-white"
                  />
                </ResponsiveColumn>
              )) : (
                <div className="mhl">
                  <p>No matches</p>
                </div>
              )}
            </div>

          </div>
          <hr className="mtn bg-dark-translucent mbxxxl" />
        </Collapsible>
      );
    },

    renderActions: function() {
      var selectedCount = this.getCheckedGroupIds().length;
      return (
        <div>
          <button type="button"
            className="button-primary mrs mbs"
            onClick={this.clearCheckedGroups}
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

    renderPublishedIntro: function() {
      if (this.getLocalBehaviorGroups().length > 0) {
        return (
          <ListHeading teamId={this.props.teamId}>
            {this.isSearching() ?
              `Skills published by Ellipsis.ai matching “${this.props.currentSearchText}”` :
              "Install skills published by Ellipsis.ai"}
          </ListHeading>
        );
      } else {
        return (
          <div>
            <ListHeading teamId={this.props.teamId} includeTeachButton={true}>
              To get started, install one of the skills published by Ellipsis.ai
            </ListHeading>

            <p className="type-blue-faded mhl mbxl">
              Each skill instructs your bot how to perform a set of related tasks, and when to respond to people in chat.
            </p>
          </div>
        );
      }
    },

    renderPublishedGroups: function() {
      var uninstalled = this.getUninstalledBehaviorGroups();
      var groups = this.getMatchingBehaviorGroupsFrom(uninstalled);
      if (this.props.publishedBehaviorGroupLoadStatus === 'loaded' && uninstalled.length === 0) {
        return (
          <div>
            <p className="phl">
              <span className="mrs">🏆💯⭐️🌈{/* <- thar be emoji invisible in intellij */}</span>
              <span>Congratulations! You’ve installed all of the skills published by Ellipsis.ai.</span>
            </p>
          </div>
        );
      } else if (this.props.publishedBehaviorGroupLoadStatus === 'loaded') {
        return (
          <div>

            {this.renderPublishedIntro()}

            <div className={"columns mvxl " + (this.props.isLoadingMatchingResults ? "pulse-faded" : "")}>
              {groups.length > 0 ? groups.map((group) => (
                <ResponsiveColumn key={group.exportId}>
                  <BehaviorGroupCard
                    name={this.highlight(group.name)}
                    description={this.getDescriptionOrMatchingTriggers(group)}
                    icon={group.icon}
                    groupData={group}
                    localId={this.getLocalIdFor(group.exportId)}
                    onBehaviorGroupImport={this.onBehaviorGroupImport}
                    onMoreInfoClick={this.toggleInfoPanel}
                    isImporting={this.isImporting(group)}
                    isImportable={true}
                    cardClassName="bg-blue-lightest"
                  />
                </ResponsiveColumn>
              )) : (
                <div className="mhl">
                  <p>No matches</p>
                </div>
              )}
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

    renderIntro: function() {
      if (this.props.localBehaviorGroups.length === 0) {
        return (
          <div className="bg-blue-medium pvxxl border-emphasis-bottom border-blue bg-large-logo">
            <div className="container container-c">
              <p className="type-l type-white phl">
                Ellipsis is a customizable chat bot that helps your team be more productive.
                Teach your bot to perform tasks and provide answers to your team.
              </p>
            </div>
          </div>
        );
      }
    },

    renderSearch: function() {
      return (
        <div className="ptxl mbxl">
          <div className="container container-c">
            <div className="mhl">
              <SearchInput
                placeholder="Search skills…"
                value={this.state.searchText}
                onChange={this.updateSearch}
                isSearching={this.props.isLoadingMatchingResults}
              />
            </div>
          </div>
        </div>
      );
    },

    render: function() {
      return (
        <div>
          <div style={{ paddingBottom: `${this.state.footerHeight}px` }}>
            {this.renderIntro()}

            <div className={(this.hasLocalBehaviorGroups() ? "bg-lightest" : "")}>
              {this.renderSearch()}

              {this.renderInstalledBehaviorGroups()}
            </div>

            <div className="container container-c mvxl">
              {this.renderPublishedGroups()}
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
                isImportable={this.selectedBehaviorGroupIsUninstalled()}
                wasImported={this.selectedBehaviorWasImported()}
                localId={this.getSelectedBehaviorGroupId()}
                onBehaviorGroupImport={this.onBehaviorGroupImport}
                onBehaviorGroupUpdate={this.onBehaviorGroupUpdate}
                updatedData={this.getUpdatedBehaviorGroupData()}
              />
            </Collapsible>
            <Collapsible
              ref="afterInstall"
              revealWhen={this.hasRecentlyInstalledBehaviorGroups() && this.getActivePanelName() === 'afterInstall'}
              animationDuration={this.getAnimationDuration()}
              onChange={this.resetFooterHeight}
            >
              <InstalledBehaviorGroupsPanel
                installedBehaviorGroups={this.getLocalBehaviorGroupsJustInstalled()}
                onToggle={this.props.onClearActivePanel}
                slackTeamId={this.props.slackTeamId}
              />
            </Collapsible>
            <Collapsible
              revealWhen={!this.props.activePanelIsModal && this.getCheckedGroupIds().length > 0}
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
                <p>{this.getTextForDeleteBehaviorGroups(this.getCheckedGroupIds().length)}</p>
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
                <p>{this.getTextForMergeBehaviorGroups(this.getCheckedGroupIds().length)}</p>
              </ConfirmActionPanel>
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  });

  return BehaviorList;
});
