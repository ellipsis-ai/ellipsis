import * as React from 'react';
import EditableName from './editable_name';
import BehaviorGroup from '../models/behavior_group';
import BehaviorVersion from '../models/behavior_version';
import BehaviorGroupCard from './behavior_group_card';
import BehaviorGroupInfoPanel from './behavior_group_info_panel';
import Collapsible from '../shared_ui/collapsible';
import ConfirmActionPanel from '../panels/confirm_action';
import SearchInput from '../form/search';
import InstalledBehaviorGroupsPanel from './installed_behavior_groups_panel';
import ListHeading from './list_heading';
import ResponsiveColumn from '../shared_ui/responsive_column';
import SubstringHighlighter from '../shared_ui/substring_highlighter';
import * as debounce from 'javascript-debounce';
import autobind from "../lib/autobind";
import {PageRequiredProps} from "../shared_ui/page";
import {PublishedBehaviorGroupLoadStatus} from "./loader";
import SidebarButton from "../form/sidebar_button";
import Sticky, {Coords} from "../shared_ui/sticky";
import {MOBILE_MAX_WIDTH} from "../lib/constants";

const ANIMATION_DURATION = 0.25;

type Props = {
  onLoadPublishedBehaviorGroups: () => void,
  onBehaviorGroupImport: (b: BehaviorGroup) => void,
  onBehaviorGroupUpdate: (orig: BehaviorGroup, upd: BehaviorGroup) => void,
  onMergeBehaviorGroups: (ids: Array<string>) => void,
  onDeleteBehaviorGroups: (ids: Array<string>) => void,
  onBehaviorGroupDeploy: (id: string) => void,
  onSearch: (text: string) => void,
  localBehaviorGroups: Array<BehaviorGroup>,
  publishedBehaviorGroups: Array<BehaviorGroup>,
  recentlyInstalled: Array<BehaviorGroup>,
  currentlyInstalling: Array<BehaviorGroup>,
  matchingResults: Array<BehaviorGroup>,
  isDeploying: boolean,
  deployError: string | null,
  currentSearchText: string,
  isLoadingMatchingResults: boolean,
  publishedBehaviorGroupLoadStatus: PublishedBehaviorGroupLoadStatus,
  teamId: string,
  slackTeamId: string,
  botName: string,
  notification: any
} & PageRequiredProps;

type State = {
  selectedBehaviorGroup: BehaviorGroup | null,
  checkedGroupIds: Array<string>,
  isSubmitting: boolean,
  searchText: string,
  visibleSection: "local" | "published"
}

class BehaviorList extends React.Component<Props, State> {
  static defaultProps: PageRequiredProps;
  delaySubmitSearch: () => void;
  delayOnScroll: () => void;
  localGroupContainer: HTMLElement | null;
  publishedGroupContainer: HTMLElement | null;
  mainHeader: HTMLElement | null;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      selectedBehaviorGroup: null,
      checkedGroupIds: [],
      isSubmitting: false,
      searchText: "",
      visibleSection: this.props.localBehaviorGroups.length > 0 ? "local" : "published"
    };

    this.delaySubmitSearch = debounce(() => this.submitSearch(), 500);
    this.delayOnScroll = debounce(() => this.onScroll(), 50);
    this.mainHeader = document.getElementById('main-header');
  }

  componentWillReceiveProps(nextProps: Props) {
    const newestImported = nextProps.recentlyInstalled.filter((next) => !BehaviorGroup.groupsIncludeExportId(this.props.recentlyInstalled, next.exportId));
    const newlyInstalled = newestImported.filter((newGroup) => !BehaviorGroup.groupsIncludeExportId(this.props.localBehaviorGroups, newGroup.exportId));
    if (newlyInstalled.length > 0 && this.props.activePanelName !== 'afterInstall') {
      this.props.onToggleActivePanel('afterInstall', true);
    }
  }

  componentDidMount() {
    this.props.onRenderNavActions(this.renderSearch());
    window.addEventListener('scroll', this.delayOnScroll);
//    window.addEventListener('resize', this.delayCheckSize);
  }

  componentDidUpdate() {
    this.props.onRenderNavActions(this.renderSearch());
  }

  onScroll() {
    const scrollY = window.scrollY;
    const localCoords = this.localGroupContainer ? this.localGroupContainer.getBoundingClientRect() : null;
    const localTop = scrollY + (localCoords ? localCoords.top : 0);
    const localHeight = localCoords ? localCoords.height : 0;
    const publishedCoords = this.publishedGroupContainer ? this.publishedGroupContainer.getBoundingClientRect() : null;
    const publishedTop = scrollY + (publishedCoords ? publishedCoords.top : 0);
    const headerHeight = this.getHeaderHeight();
    const visibleTop = scrollY + headerHeight;
    const visibleBottom = scrollY + window.innerHeight;
    const visibleOneThird = visibleTop + Math.round((visibleBottom - visibleTop) / 3);
    if (publishedTop > visibleOneThird || localHeight > 0 && localTop >= visibleTop) {
      this.setState({
        visibleSection: "local"
      });
    } else {
      this.setState({
        visibleSection: "published"
      })
    }
  }

  getHeaderHeight(): number {
    return this.mainHeader ? this.mainHeader.offsetHeight : 0;
  }

  scrollToElement(element: HTMLElement): void {
    const elementRect = element.getBoundingClientRect();
    const newY = Math.max(window.scrollY + elementRect.top - this.getHeaderHeight(), 0);
    window.scrollTo(window.scrollX, newY);
  }

  scrollToLocal(): void {
    if (this.localGroupContainer) {
      this.scrollToElement(this.localGroupContainer);
    }
  }

  scrollToPublished(): void {
    if (this.publishedGroupContainer) {
      this.scrollToElement(this.publishedGroupContainer);
    }
  }

  isScrolledToLocal(): boolean {
    return this.state.visibleSection === "local";
  }

  isScrolledToPublished(): boolean {
    return this.state.visibleSection === "published";
  }

  getSidebarCoordinates(): Coords {
    const headerHeight = this.getHeaderHeight();
    const footerHeight = this.props.activePanelIsModal ? 0 : this.props.footerHeight;
    const windowHeight = window.innerHeight;

    const availableHeight = windowHeight - headerHeight - footerHeight;
    const newHeight = availableHeight > 0 ? availableHeight : window.innerHeight;
    return {
      top: headerHeight,
      left: window.scrollX > 0 ? -window.scrollX : 0,
      bottom: newHeight
    };
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

  getMostRecentGroupJustInstalled(): BehaviorGroup | null {
    const allInstalled = this.getLocalBehaviorGroupsJustInstalled();
    const numInstalled = allInstalled.length;
    return numInstalled > 0 ? allInstalled[numInstalled - 1] : null;
  }

  getCheckedGroupIds(): Array<string> {
    return this.state.checkedGroupIds || [];
  }

  getLocalIdFor(exportId: string | null): string | null {
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

  getSelectedBehaviorGroup(): BehaviorGroup | null {
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

  getSelectedBehaviorGroupId(): string | null {
    var group = this.getSelectedBehaviorGroup();
    return group ? group.id : null;
  }

  hasRecentlyInstalledBehaviorGroups(): boolean {
    return this.getLocalBehaviorGroupsJustInstalled().length > 0;
  }

  getActivePanelName(): string | null {
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

  getUpdatedBehaviorGroupData(): BehaviorGroup | null {
    const selected = this.getSelectedBehaviorGroup();
    if (selected && selected.exportId && selected.id) {
      return this.props.publishedBehaviorGroups.find((ea) => ea.exportId === selected.exportId) || null;
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

  highlight(text: string | null) {
    if (text) {
      return (
        <SubstringHighlighter text={text} substring={this.props.currentSearchText}/>
      );
    } else {
      return null;
    }
  }

  getDescriptionOrMatchingTriggers(group: BehaviorGroup) {
    var lowercaseDescription = group.getDescription().toLowerCase();
    var lowercaseSearch = this.props.currentSearchText.toLowerCase();
    var matchingBehaviorVersions: Array<BehaviorVersion> = [];
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

  renderInstalledBehaviorGroups(groups: Array<BehaviorGroup>, hasLocalGroups: boolean) {
    return (
      <Collapsible revealWhen={hasLocalGroups} animationDuration={0.5}>
        <div className="container container-c ptxl">

          <ListHeading
            heading={this.isSearching() ?
              `Your team’s skills matching “${this.props.currentSearchText}”` :
              "Your team’s skills"
            }
            sideContent={this.renderTeachButton()}
          />

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
        <hr className="mvn bg-dark-translucent"/>
      </Collapsible>
    );
  }

  renderActions() {
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

  renderTeachButton() {
    return (
      <a href={jsRoutes.controllers.BehaviorEditorController.newGroup(this.props.teamId).url}
        className="button button-shrink">
        Teach Ellipsis something new…
      </a>
    );
  }

  renderPublishedIntro() {
    if (this.getLocalBehaviorGroups().length === 0) {
      return (
        <div>
          <ListHeading
            heading={"To get started, install one of the skills available"}
            sideContent={this.renderTeachButton()}
          />

          <p className="type-blue-faded mhl mbxl">
            Each skill instructs your bot how to perform a set of related tasks, and when to respond to people in chat.
          </p>
        </div>
      );
    } else {
      return (
        <div>
          <ListHeading heading={this.isSearching() ?
            `Available skills matching “${this.props.currentSearchText}”` :
            "Skills available to install"
          } />
        </div>
      );
    }
  }

  renderPublishedGroups(groups: Array<BehaviorGroup>, hasUninstalledGroups: boolean) {
    if (this.props.publishedBehaviorGroupLoadStatus === 'loaded' && !hasUninstalledGroups) {
      return (
        <div>
          <p className="phl">
            <span className="mrs">🏆💯⭐️🌈</span>
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
    } else {
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

  renderIntro() {
    if (this.getLocalBehaviorGroups().length === 0) {
      return (
        <div className="bg-blue-medium pvxxl border-bottom-thick border-blue type-white">
          <div className="container">
            <div className="type-l type-light">
              Ellipsis is a customizable bot that helps your team be more productive.
              Teach your bot to perform tasks and provide answers to your team.
            </div>
          </div>
        </div>
      );
    } else {
      return null;
    }
  }

  renderSearch() {
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

  render() {
    const allLocal = this.getLocalBehaviorGroups();
    const hasLocalGroups = allLocal.length > 0;
    const localGroups = this.getMatchingBehaviorGroupsFrom(allLocal);
    const allUninstalled = this.getUninstalledBehaviorGroups();
    const uninstalledGroups = this.getMatchingBehaviorGroupsFrom(allUninstalled);
    const hasUninstalledGroups = allUninstalled.length > 0;
    return (
      <div className="flex-row-cascade">
        {this.props.notification}
        <div className="flex-row-cascade" style={{ paddingBottom: `${this.props.footerHeight}px` }}>
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide phn">
              <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
                <div className="column column-page-sidebar flex-column flex-column-left bg-white border-right-thick border-light prn mobile-display-none">
                  <Sticky
                    onGetCoordinates={this.getSidebarCoordinates}
                    disabledWhen={() => window.innerWidth <= MOBILE_MAX_WIDTH}
                  >
                    <div className="pvxxl mobile-pvl">
                      <SidebarButton
                        onClick={this.scrollToLocal}
                        selected={this.isScrolledToLocal()}
                        disabled={!hasLocalGroups}
                        className="mbl mobile-mbm"
                      >
                        Your team’s skills
                      </SidebarButton>

                      <SidebarButton
                        onClick={this.scrollToPublished}
                        selected={this.isScrolledToPublished()}
                        disabled={!hasUninstalledGroups}
                      >
                        Skills available to install
                      </SidebarButton>
                    </div>
                  </Sticky>
                </div>
                <div className="column column-page-main column-page-main-wide flex-column flex-column-main">
                  {this.renderIntro()}

                  <div ref={(el) => this.localGroupContainer = el} className="bg-lightest">
                    {this.renderInstalledBehaviorGroups(localGroups, hasLocalGroups)}
                  </div>

                  <div ref={(el) => this.publishedGroupContainer = el} className="container container-c ptxxl pbxl">
                    {this.renderPublishedGroups(uninstalledGroups, hasUninstalledGroups)}
                  </div>
                </div>
              </div>
            </div>
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
                installedBehaviorGroup={this.getMostRecentGroupJustInstalled()}
                onToggle={this.props.onClearActivePanel}
                onDeploy={this.props.onBehaviorGroupDeploy}
                isDeploying={this.props.isDeploying}
                deployError={this.props.deployError}
                slackTeamId={this.props.slackTeamId}
                botName={this.props.botName}
              />
            </Collapsible>
            <Collapsible
              revealWhen={!this.props.activePanelIsModal && this.getCheckedGroupIds().length > 0}
            >
              <div className="border-top">
                <div className="container ptm">
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

export default BehaviorList;
