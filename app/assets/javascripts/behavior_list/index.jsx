define(function(require) {
  var React = require('react'),
    BehaviorName = require('./behavior_name'),
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
    PageWithPanels = require('../shared_ui/page_with_panels'),
    ResponsiveColumn = require('../shared_ui/responsive_column'),
    SubstringHighlighter = require('../shared_ui/substring_highlighter'),
    debounce = require('javascript-debounce');

  const ANIMATION_DURATION = 0.25;

  const BehaviorList = React.createClass({
    displayName: "BehaviorList",
    propTypes: Object.assign(PageWithPanels.requiredPropTypes(), {
      onLoadPublishedBehaviorGroups: React.PropTypes.func.isRequired,
      onBehaviorGroupImport: React.PropTypes.func.isRequired,
      onMergeBehaviorGroups: React.PropTypes.func.isRequired,
      onDeleteBehaviorGroups: React.PropTypes.func.isRequired,
      onSearch: React.PropTypes.func.isRequired,
      behaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)).isRequired,
      publishedBehaviorGroups: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)),
      recentlyInstalled: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)),
      matchingResults: React.PropTypes.arrayOf(React.PropTypes.instanceOf(BehaviorGroup)),
      currentSearchText: React.PropTypes.string.isRequired,
      isLoadingMatchingResults: React.PropTypes.bool.isRequired,
      publishedBehaviorGroupLoadStatus: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      slackTeamId: React.PropTypes.string.isRequired
    }),

    getInitialState: function() {
      return {
        selectedBehaviorGroup: null,
        selectedGroupIds: [],
        isSubmitting: false,
        footerHeight: 0,
        importingList: [],
        searchText: ""
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

    getBehaviorGroups: function() {
      return this.props.behaviorGroups.concat(this.props.recentlyInstalled);
    },

    hasLocalBehaviorGroups: function() {
      return this.getBehaviorGroups().length > 0;
    },

    getMatchingBehaviorGroups: function() {
      if (this.props.matchingResults.length > 0) {
        return this.getBehaviorGroups().filter((ea) =>
          BehaviorGroup.groupsIncludeExportId(this.props.matchingResults, ea.exportId)
        );
      } else {
        return this.getBehaviorGroups();
      }
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

    mergeBehaviorGroups: function() {
      this.setState({
        isSubmitting: true
      }, () => {
        this.props.onMergeBehaviorGroups(this.getSelectedGroupIds());
      });
    },

    deleteBehaviorGroups: function() {
      this.setState({
        isSubmitting: true
      }, () => {
        this.props.onDeleteBehaviorGroups(this.getSelectedGroupIds());
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
      const candidates = this.props.publishedBehaviorGroups.filter((published) =>
        !BehaviorGroup.groupsIncludeExportId(this.props.behaviorGroups, published.exportId)
      );

      if (this.props.matchingResults.length > 0) {
        return candidates.filter((ea) =>
          BehaviorGroup.groupsIncludeExportId(this.props.matchingResults, ea.exportId)
        );
      } else {
        return candidates;
      }
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
              <BehaviorName
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
      var groups = this.getMatchingBehaviorGroups();
      return (
        <Collapsible revealWhen={this.hasLocalBehaviorGroups()} animationDuration={0.5}>
          <div className="container container-c mvxl">

            <ListHeading teamId={this.props.teamId} includeTeachButton={true}>
              {this.props.matchingResults.length ?
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
                    onSelectChange={this.onGroupSelectionCheckboxChange}
                    isSelected={this.isGroupSelected(group.id)}
                    cardClassName="bg-white"
                  />
                </ResponsiveColumn>
              )) : (
                <div className="mhl">
                  <p>No matches.</p>
                </div>
              )}
            </div>

          </div>
          <hr className="mtn bg-dark-translucent mbxxxl" />
        </Collapsible>
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

    renderPublishedIntro: function() {
      if (this.getBehaviorGroups().length > 0) {
        return (
          <ListHeading teamId={this.props.teamId}>
            {this.props.matchingResults.length === 0 ?
              "Install skills published by Ellipsis.ai" :
              `Skills published by Ellipsis.ai matching “${this.props.currentSearchText}”`}
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
      var groups = this.getUninstalledBehaviorGroups();
      if (this.props.publishedBehaviorGroupLoadStatus === 'loaded' && groups.length === 0 && this.props.matchingResults.length === 0) {
        return (
          <div>
            <p className="phl">
              <span className="mrs">🏆💯⭐️🌈{/* <- thar be emoji invisible in intellij */}</span>
              <span>Congratulations! You’ve installed all of the skills published by Ellipsis.ai.</span>
            </p>
          </div>
        );
      } else if (this.props.publishedBehaviorGroupLoadStatus === 'loaded' && groups.length > 0) {
        return (
          <div>

            {this.renderPublishedIntro()}

            <div className={"columns mvxl " + (this.props.isLoadingMatchingResults ? "pulse-faded" : "")}>
              {groups.map((group) => (
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

    renderIntro: function() {
      if (this.props.behaviorGroups.length === 0) {
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
                isImportable={this.selectedBehaviorGroupIsImportable()}
                wasImported={this.selectedBehaviorWasImported()}
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
