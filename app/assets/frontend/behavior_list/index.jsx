// @flow
import * as React from 'react';
import EditableName from './editable_name';
import BehaviorGroup from '../models/behavior_group';
import BehaviorGroupCard from './behavior_group_card';
import BehaviorGroupInfoPanel from './behavior_group_info_panel';
import Collapsible from '../shared_ui/collapsible';
import ConfirmActionPanel from '../panels/confirm_action';
import SearchInput from '../form/search';
import InstalledBehaviorGroupsPanel from './installed_behavior_groups_panel';
import ListHeading from './list_heading';
import Page from '../shared_ui/page';
import ResponsiveColumn from '../shared_ui/responsive_column';
import SubstringHighlighter from '../shared_ui/substring_highlighter';
import debounce from 'javascript-debounce';
import autobind from "../lib/autobind";
import type {PageRequiredProps} from "../shared_ui/page";

const ANIMATION_DURATION = 0.25;

type Props = {
  onLoadPublishedBehaviorGroups: () => void,
  onBehaviorGroupImport: (BehaviorGroup) => void,
  onBehaviorGroupUpdate: (BehaviorGroup, BehaviorGroup) => void,
  onMergeBehaviorGroups: (ids: Array<string>) => void,
  onDeleteBehaviorGroups: (ids: Array<string>) => void,
  onSearch: (text: string) => void,
  localBehaviorGroups: Array<BehaviorGroup>,
  publishedBehaviorGroups: Array<BehaviorGroup>,
  recentlyInstalled: Array<BehaviorGroup>,
  currentlyInstalling: Array<BehaviorGroup>,
  matchingResults: Array<BehaviorGroup>,
  currentSearchText: string,
  isLoadingMatchingResults: boolean,
  publishedBehaviorGroupLoadStatus: string,
  teamId: string,
  slackTeamId: string,
  botName: string,
  notification: ?React.Node
} & PageRequiredProps;

type State = {
  selectedBehaviorGroup: ?BehaviorGroup,
  checkedGroupIds: Array<string>,
  isSubmitting: boolean,
  searchText: string
}

class BehaviorList extends React.Component<Props, State> {
  static defaultProps: PageRequiredProps;
  delaySubmitSearch: () => void;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      selectedBehaviorGroup: null,
      checkedGroupIds: [],
      isSubmitting: false,
      searchText: ""
    };

    this.delaySubmitSearch = debounce(function() {
      this.submitSearch();
    }, 500);
  }

  componentWillReceiveProps(nextProps: Props) {
    const newestImported = nextProps.recentlyInstalled.filter((next) => !BehaviorGroup.groupsIncludeExportId(this.props.recentlyInstalled, next.exportId));
    const newlyInstalled = newestImported.filter((newGroup) => !BehaviorGroup.groupsIncludeExportId(this.props.localBehaviorGroups, newGroup.exportId));
    if (newlyInstalled.length > 0 && this.props.activePanelName !== 'afterInstall') {
      this.props.onToggleActivePanel('afterInstall');
    }
  }

  componentDidMount() {
    this.props.onRenderNavActions(this.renderSearch());
  }

  componentDidUpdate() {
    this.props.onRenderNavActions(this.renderSearch());
  }

  updateSearch(newValue: string) {
    this.setState({
      searchText: newValue
    }, () => {
      if (newValue) {
        this.delaySubmitSearch();
      } else {
        this.submitSearch();
      }
    });
  }

  submitSearch() {
    this.props.onSearch(this.state.searchText);
  }

  getAnimationDuration() {
    return ANIMATION_DURATION;
  }

  getLocalBehaviorGroups(): Array<BehaviorGroup> {
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
  }

  isSearching(): boolean {
    return Boolean(this.props.currentSearchText && this.props.currentSearchText.length);
  }

  getMatchingBehaviorGroupsFrom(groups: Array<BehaviorGroup>): Array<BehaviorGroup> {
    if (this.isSearching()) {
      return groups.filter((ea) =>
        ea.exportId && BehaviorGroup.groupsIncludeExportId(this.props.matchingResults, ea.exportId)
      );
    } else {
      return groups;
    }
  }

  getUninstalledBehaviorGroups(): Array<BehaviorGroup> {
    return this.props.publishedBehaviorGroups.filter((published) =>
      published.exportId && !BehaviorGroup.groupsIncludeExportId(this.props.localBehaviorGroups, published.exportId)
    );
  }

  getLocalBehaviorGroupsJustInstalled(): Array<BehaviorGroup> {
    return this.props.recentlyInstalled;
  }

  getCheckedGroupIds(): Array<string> {
    return this.state.checkedGroupIds || [];
  }

  getLocalIdFor(exportId: ?string): ?string {
    var localGroup = exportId ? this.getLocalBehaviorGroups().find((ea) => ea.exportId === exportId) : null;
    return localGroup ? localGroup.id : null;
  }

  isGroupChecked(group: BehaviorGroup): boolean {
    return Boolean(group.id && this.getCheckedGroupIds().indexOf(group.id) >= 0);
  }

  confirmDeleteBehaviorGroups(): void {
    this.toggleActivePanel('confirmDeleteBehaviorGroups', true);
  }

  confirmMergeBehaviorGroups(): void {
    this.toggleActivePanel('confirmMergeBehaviorGroups', true);
  }

  onGroupCheckboxChange(groupId: string, isChecked: boolean, optionalCallback?: () => void): void {
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
  }

  clearCheckedGroups(): void {
    this.setState({
      checkedGroupIds: []
    });
  }

  mergeBehaviorGroups(): void {
    this.setState({
      isSubmitting: true
    }, () => {
      this.props.onMergeBehaviorGroups(this.getCheckedGroupIds());
    });
  }

  deleteBehaviorGroups(): void {
    this.setState({
      isSubmitting: true
    }, () => {
      this.props.onDeleteBehaviorGroups(this.getCheckedGroupIds());
    });
  }

  getActionsLabel(checkedCount: number): string {
    if (checkedCount === 0) {
      return "No skills selected";
    } else if (checkedCount === 1) {
      return "1 skill selected";
    } else {
      return `${checkedCount} skills selected`;
    }
  }

  getLabelForDeleteAction(checkedCount: number): string {
    if (checkedCount < 2) {
      return "Delete skill";
    } else {
      return `Delete skills`;
    }
  }

  getTextForDeleteBehaviorGroups(checkedCount: number): string {
    if (checkedCount === 1) {
      return "Are you sure you want to delete this skill?";
    } else {
      return `Are you sure you want to delete these ${checkedCount} skills?`;
    }
  }

  getTextForMergeBehaviorGroups(checkedCount: number): string {
    return `Are you sure you want to merge these ${checkedCount} skills?`;
  }

  getSelectedBehaviorGroup(): ?BehaviorGroup {
    return this.state.selectedBehaviorGroup;
  }

  selectedBehaviorGroupIsUninstalled(): boolean {
    var selectedGroup = this.getSelectedBehaviorGroup();
    return Boolean(selectedGroup && selectedGroup.exportId && !this.getLocalIdFor(selectedGroup.exportId));
  }

  selectedBehaviorWasImported(): boolean {
    var selectedGroup = this.getSelectedBehaviorGroup();
    return Boolean(
      selectedGroup &&
      selectedGroup.id &&
      selectedGroup.exportId &&
      BehaviorGroup.groupsIncludeExportId(this.props.publishedBehaviorGroups, selectedGroup.exportId)
    );
  }

  getSelectedBehaviorGroupId(): ?string {
    var group = this.getSelectedBehaviorGroup();
    return group ? group.id : null;
  }

  hasRecentlyInstalledBehaviorGroups(): boolean {
    return this.getLocalBehaviorGroupsJustInstalled().length > 0;
  }

  getActivePanelName(): ?string {
    return this.props.activePanelName;
  }

  clearActivePanel(): void {
    this.props.onClearActivePanel();
  }

  toggleActivePanel(panelName: string, beModal?: boolean): void {
    this.props.onToggleActivePanel(panelName, beModal);
  }

  onBehaviorGroupImport(groupToInstall: BehaviorGroup): void {
    if (this.getActivePanelName() === 'moreInfo') {
      this.clearActivePanel();
    }
    this.props.onBehaviorGroupImport(groupToInstall);
  }

  onBehaviorGroupUpdate(existingGroup: BehaviorGroup, updatedData: BehaviorGroup): void {
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
  }

  getUpdatedBehaviorGroupData(): ?BehaviorGroup {
    const selected = this.getSelectedBehaviorGroup();
    if (selected && selected.exportId && selected.id) {
      return this.props.publishedBehaviorGroups.find((ea) => ea.exportId === selected.exportId);
    } else {
      return null;
    }
  }

  isImporting(group: BehaviorGroup): boolean {
    return Boolean(group.exportId && BehaviorGroup.groupsIncludeExportId(this.props.currentlyInstalling, group.exportId));
  }

  wasReimported(group: BehaviorGroup): boolean {
    const exportId = group.exportId;
    return Boolean(exportId && BehaviorGroup.groupsIncludeExportId(this.props.localBehaviorGroups, exportId) &&
      BehaviorGroup.groupsIncludeExportId(this.props.recentlyInstalled, exportId));
  }

  toggleInfoPanel(group: BehaviorGroup): void {
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
  }

  highlight(text: ?string): React.Node {
    if (text) {
      return (
        <SubstringHighlighter text={text} substring={this.props.currentSearchText}/>
      );
    } else {
      return null;
    }
  }

  getDescriptionOrMatchingTriggers(group: BehaviorGroup): React.Node {
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
          {matchingBehaviorVersions.map((version, index) => (
            <EditableName
              className="mbs"
              version={version}
              disableLink={true}
              key={`matchingBehaviorVersion${version.behaviorId || version.exportId || index}`}
              highlightText={this.props.currentSearchText}
            />
          ))}
        </div>
      );
    }
  }

  renderInstalledBehaviorGroups(): React.Node {
    var allLocal = this.getLocalBehaviorGroups();
    var groups = this.getMatchingBehaviorGroupsFrom(allLocal);
    return (
      <Collapsible revealWhen={allLocal.length > 0} animationDuration={0.5}>
        <div className="container container-c ptxl">

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
        <hr className="mtn bg-dark-translucent mbxxxl"/>
      </Collapsible>
    );
  }

  renderActions(): React.Node {
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
  }

  renderPublishedIntro(): React.Node {
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
  }

  renderPublishedGroups(): React.Node {
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
  }

  renderIntro(): React.Node {
    if (this.props.localBehaviorGroups.length === 0) {
      return (
        <div className="bg-blue-medium pvxxl border-bottom-thick border-blue type-white">
          <div className="container container-c">
            <div className="type-l type-light phl">
              Ellipsis is a customizable bot that helps your team be more productive.
              Teach your bot to perform tasks and provide answers to your team.
            </div>
          </div>
        </div>
      );
    }
  }

  renderSearch(): React.Node {
    return (
      <div className="pts display-inline-block width-15">
        <SearchInput
          placeholder="Search skills…"
          value={this.state.searchText}
          onChange={this.updateSearch}
          isSearching={this.props.isLoadingMatchingResults}
        />
      </div>
    );
  }

  render(): React.Node {
    return (
      <div>
        {this.props.notification}
        <div style={{ paddingBottom: `${this.props.footerHeight}px` }}>
          {this.renderIntro()}

          <div className="bg-lightest">
            {this.renderInstalledBehaviorGroups()}
          </div>

          <div className="container container-c mvxl">
            {this.renderPublishedGroups()}
          </div>
        </div>

        {this.props.onRenderFooter((
          <div>
            <Collapsible
              revealWhen={this.getActivePanelName() === 'moreInfo'}
              animationDuration={this.getAnimationDuration()}
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
              revealWhen={this.hasRecentlyInstalledBehaviorGroups() && this.getActivePanelName() === 'afterInstall'}
              animationDuration={this.getAnimationDuration()}
            >
              <InstalledBehaviorGroupsPanel
                installedBehaviorGroups={this.getLocalBehaviorGroupsJustInstalled()}
                onToggle={this.props.onClearActivePanel}
                slackTeamId={this.props.slackTeamId}
                botName={this.props.botName}
              />
            </Collapsible>
            <Collapsible
              revealWhen={!this.props.activePanelIsModal && this.getCheckedGroupIds().length > 0}
            >
              <div className="border-top">
                <div className="container container-c ptm">
                  {this.renderActions()}
                </div>
              </div>
            </Collapsible>
            <Collapsible revealWhen={this.getActivePanelName() === 'confirmDeleteBehaviorGroups'}>
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
            <Collapsible revealWhen={this.getActivePanelName() === 'confirmMergeBehaviorGroups'}>
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
          </div>
        ))}
      </div>
    );
  }
}

BehaviorList.defaultProps = Page.requiredPropDefaults();

export default BehaviorList;
