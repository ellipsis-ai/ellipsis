import * as React from 'react';
import animateScrollTo from 'animated-scroll-to';
import BehaviorGroup from '../models/behavior_group';
import BehaviorGroupCard from './behavior_group_card';
import BehaviorGroupInfoPanel from './behavior_group_info_panel';
import Checkbox from '../form/checkbox';
import Collapsible from '../shared_ui/collapsible';
import ConfirmActionPanel from '../panels/confirm_action';
import SearchInput from '../form/search';
import InstalledBehaviorGroupsPanel from './installed_behavior_groups_panel';
import ListHeading from './list_heading';
import ResponsiveColumn from '../shared_ui/responsive_column';
import * as debounce from 'javascript-debounce';
import autobind from "../lib/autobind";
import {PageRequiredProps} from "../shared_ui/page";
import {PublishedBehaviorGroupLoadStatus} from "./loader";
import Sticky, {Coords} from "../shared_ui/sticky";
import {MOBILE_MAX_WIDTH} from "../lib/constants";
import Button from "../form/button";
import {SearchResult} from "./loader";
import SVGInstall from '../svg/install';
import SVGInstalled from '../svg/installed';
import SVGInstalling from '../svg/installing';
import FixedHeader from "../shared_ui/fixed_header";
import {CSSProperties} from "react";
import {maybeDiffFor} from "../models/diffs";
import {ToggleGroup, ToggleGroupItem} from '../form/toggle_group';
import ImportFromGithubPanel from "./import_from_github_panel";

const ANIMATION_DURATION = 0.25;

type Props = {
  onLoadPublishedBehaviorGroups: () => void,
  onBehaviorGroupImport: (b: BehaviorGroup) => void,
  onBehaviorGroupUpdate: (orig: BehaviorGroup, upd: BehaviorGroup, callback?: (newGroup: BehaviorGroup) => void) => void,
  onMergeBehaviorGroups: (ids: Array<string>) => void,
  onDeleteBehaviorGroups: (ids: Array<string>) => void,
  onBehaviorGroupDeploy: (id: string) => void,
  onSearch: (text: string) => void,
  localBehaviorGroups: Array<BehaviorGroup>,
  publishedBehaviorGroups: Array<BehaviorGroup>,
  recentlyInstalled: Array<BehaviorGroup>,
  currentlyInstalling: Array<BehaviorGroup>,
  matchingResults: {
    [searchText: string]: SearchResult | undefined
  },
  isDeploying: boolean,
  deployError: Option<string>,
  publishedBehaviorGroupLoadStatus: PublishedBehaviorGroupLoadStatus,
  teamId: string,
  slackTeamId: string,
  botName: string,
  csrfToken: string
  notification: any
} & PageRequiredProps;

interface PublishedBehaviorGroupDiffers {
  [behaviorGroupId: string]: boolean
}

type State = {
  selectedBehaviorGroup: Option<BehaviorGroup>,
  checkedGroupIds: Array<string>,
  isSubmitting: boolean,
  userSearchText: string,
  activeSearchText: string,
  visibleSection: "local" | "published",
  searchHeaderHeight: number,
  onlyShowManaged: boolean,
  publishedBehaviorGroupDiffers: PublishedBehaviorGroupDiffers
}

class BehaviorList extends React.Component<Props, State> {
  static defaultProps: PageRequiredProps;
  delaySubmitSearch: () => void;
  delayUpdateActiveSearch: (newText: string) => void;
  delayOnScroll: () => void;
  localGroupContainer: Option<HTMLElement>;
  publishedGroupContainer: Option<HTMLElement>;
  mainHeader: Option<HTMLElement>;
  localHeading: Option<HTMLElement>;
  publishedHeading: Option<HTMLElement>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      selectedBehaviorGroup: null,
      checkedGroupIds: [],
      isSubmitting: false,
      userSearchText: "",
      activeSearchText: "",
      visibleSection: this.props.localBehaviorGroups.length > 0 ? "local" : "published",
      searchHeaderHeight: 0,
      onlyShowManaged: false,
      publishedBehaviorGroupDiffers: {}
    };

    this.delaySubmitSearch = debounce(() => this.submitSearch(), 50);
    this.delayUpdateActiveSearch = debounce((newText) => this.updateActiveSearch(newText), 200);
    this.delayOnScroll = debounce(() => this.onScroll(), 50);
    this.mainHeader = document.getElementById('main-header');
  }

  componentWillReceiveProps(nextProps: Props) {
    const newestImported = nextProps.recentlyInstalled.filter((next) => {
      return !BehaviorGroup.groupsIncludeExportId(this.props.recentlyInstalled, next.exportId);
    });
    const newlyInstalled = newestImported.filter((newGroup) => {
      return !BehaviorGroup.groupsIncludeExportId(this.props.localBehaviorGroups, newGroup.exportId);
    });
    const newestUpdated = newestImported.filter((newGroup) => {
      return BehaviorGroup.groupsIncludeExportId(this.props.localBehaviorGroups, newGroup.exportId);
    });
    if (newlyInstalled.length > 0 && this.props.activePanelName !== 'afterInstall') {
      this.props.onToggleActivePanel('afterInstall', true);
    } else if (newestUpdated.length > 0) {
      const selected = this.getSelectedBehaviorGroup();
      const selectedUpdated = selected && newestUpdated.find((ea) => ea.id === selected.id);
      if (selected && selectedUpdated) {
        this.setState({
          selectedBehaviorGroup: selectedUpdated
        });
      }
    }
    if (nextProps.publishedBehaviorGroups.every((published) => {
      return nextProps.recentlyInstalled.some((recent) => recent.exportId === published.exportId)
    })) {
      this.setState({
        visibleSection: "local"
      });
    }
  }

  componentDidMount() {
    this.props.onRenderNavActions(this.renderNavActions());
    window.addEventListener('scroll', this.delayOnScroll);
  }

  componentDidUpdate() {
    this.props.onRenderNavActions(this.renderNavActions());
  }

  getSearchText(): string {
    return this.state.activeSearchText;
  }

  toggleGithubImport(): void {
    this.toggleActivePanel("importFromGithub", true);
  }

  renderNavActions() {
    return (
      <div className="mtl">
        <a href={jsRoutes.controllers.BehaviorEditorController.newGroup(this.props.teamId).url}
          className="button button-s button-shrink mrs">
          Create new skill…
        </a>
        <Button className="button-s button-shrink" onClick={this.toggleGithubImport}>Import skill from GitHub…</Button>
      </div>
    );
  }

  onScroll() {
    const scrollY = window.scrollY;
    const localCoords = this.localGroupContainer ? this.localGroupContainer.getBoundingClientRect() : null;
    const localTop = scrollY + (localCoords ? localCoords.top : 0);
    const localHeight = localCoords ? localCoords.height : 0;
    const localBottom = localTop + localHeight;
    const localHeadingCoords = this.localHeading ? this.localHeading.getBoundingClientRect() : null;
    const localHeadingTop = scrollY + (localHeadingCoords ? localHeadingCoords.top : 0);
    const publishedHeadingCoords = this.publishedHeading ? this.publishedHeading.getBoundingClientRect() : null;
    const publishedHeadingTop = scrollY + (publishedHeadingCoords ? publishedHeadingCoords.top : 0);
    const publishedHeadingHeight = publishedHeadingCoords ? publishedHeadingCoords.height : 0;
    const publishedHeadingBottom = publishedHeadingTop + publishedHeadingHeight;
    const headerHeight = this.getHeaderHeight();
    const visibleTop = scrollY + headerHeight;
    const visibleBottom = scrollY + window.innerHeight;
    const visibleHeight = visibleBottom - visibleTop;
    const visibleHalf = visibleTop + Math.round((visibleBottom - visibleTop) / 2);

    const publishedNotVisible = !this.publishedHeading || publishedHeadingBottom > visibleBottom;
    const localContainerIsSmall = this.localHeading && localHeight < visibleHeight;
    const localHeadingBelowTop = this.localHeading && localHeadingTop >= visibleTop;
    const localBottomVisibleBelowHalf = this.localHeading && localBottom >= visibleHalf;
    if (publishedNotVisible || (localContainerIsSmall &&
        (localHeadingBelowTop || localBottomVisibleBelowHalf))) {
      this.setState({
        visibleSection: "local"
      });
    } else {
      this.setState({
        visibleSection: "published"
      })
    }
  }

  updateSearchHeaderHeight(newHeight: number): void {
    this.setState({
      searchHeaderHeight: newHeight
    });
  }

  getMainHeaderHeight(): number {
    return this.mainHeader ? this.mainHeader.offsetHeight : 0;
  }

  getHeaderHeight(): number {
    return this.getMainHeaderHeight() + this.state.searchHeaderHeight;
  }

  getScrollableContainerStyle(): CSSProperties {
    return {
      paddingTop: `${this.state.searchHeaderHeight}px`,
      paddingBottom: `${this.props.footerHeight}px`
    }
  }

  scrollToElement(element: HTMLElement): void {
    const elementRect = element.getBoundingClientRect();
    const newY = Math.max(window.scrollY + elementRect.top - this.getHeaderHeight(), 0);
    animateScrollTo(newY, {
      speed: 350,
      minDuration: 100,
      maxDuration: 500,
      cancelOnUserAction: true
    });
  }

  scrollToLocal(): void {
    const localContainer = this.localGroupContainer;
    if (localContainer) {
      this.setState({
        visibleSection: "local"
      }, () => {
        this.scrollToElement(localContainer);
      });
    }
  }

  scrollToPublished(): void {
    const publishedContainer = this.publishedGroupContainer;
    if (publishedContainer) {
      this.setState({
        visibleSection: "published"
      }, () => {
        this.scrollToElement(publishedContainer);
      });
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

  updateUserSearch(newValue: string, optionalCallback?: () => void) {
    this.setState({
      userSearchText: newValue
    }, () => {
      if (newValue) {
        this.delaySubmitSearch();
      }
      this.delayUpdateActiveSearch(newValue);
      if (optionalCallback) {
        optionalCallback();
      }
    });
  }

  showAllSkills() {
    this.setState({
      onlyShowManaged: false
    });
  }

  showManagedSkills() {
    this.setState({
      onlyShowManaged: true
    });
  }

  submitSearch() {
    this.props.onSearch(this.state.userSearchText);
  }

  updateActiveSearch(newValue: string) {
    this.setState({
      activeSearchText: newValue.trim()
    })
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
    const searchText = this.getSearchText();
    return Boolean(searchText && searchText.length);
  }

  getCurrentSearchResult(): SearchResult | undefined {
    return this.props.matchingResults[this.getSearchText()];
  }

  getResultsForSearch(): Array<BehaviorGroup> {
    const result = this.getCurrentSearchResult();
    if (result) {
      return result.matches
    } else {
      return [];
    }
  }

  isLoadingMatchingResults(): boolean {
    const result = this.getCurrentSearchResult();
    if (result) {
      return result.isLoading;
    } else {
      return false;
    }
  }

  getMatchingBehaviorGroupsFrom(groups: Array<BehaviorGroup>): Array<BehaviorGroup> {
    if (this.isSearching() && !this.isLoadingMatchingResults()) {
      return groups.filter((ea) =>
        ea.exportId && BehaviorGroup.groupsIncludeExportId(this.getResultsForSearch(), ea.exportId)
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

  getMostRecentGroupJustInstalled(): Option<BehaviorGroup> {
    const allInstalled = this.getLocalBehaviorGroupsJustInstalled();
    const numInstalled = allInstalled.length;
    return numInstalled > 0 ? allInstalled[numInstalled - 1] : null;
  }

  getCheckedGroupIds(): Array<string> {
    return this.state.checkedGroupIds || [];
  }

  getLocalIdFor(exportId: Option<string>): Option<string> {
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

  getSelectedBehaviorGroup(): Option<BehaviorGroup> {
    return this.state.selectedBehaviorGroup;
  }

  behaviorGroupIsUninstalled(selectedGroup: Option<BehaviorGroup>): boolean {
    return Boolean(selectedGroup && selectedGroup.exportId && !this.getLocalIdFor(selectedGroup.exportId));
  }

  publishedGroupDataFor(group: Option<BehaviorGroup>): Option<BehaviorGroup> {
    return group &&
      group.id &&
      group.exportId &&
      this.props.publishedBehaviorGroups.find((ea) => ea.exportId === group.exportId) ||
      null;
  }

  getUpdatedPublishedGroupDiffersState(groupId: string, differs: boolean): PublishedBehaviorGroupDiffers {
    const newState: PublishedBehaviorGroupDiffers = {};
    newState[groupId] = differs;
    return Object.assign({}, this.state.publishedBehaviorGroupDiffers, newState);
  }

  hasDiffForPublished(local: BehaviorGroup, published: BehaviorGroup): boolean {
    return Boolean(maybeDiffFor(local, published, null, true));
  }

  publishedGroupDataDiffersFor(group: Option<BehaviorGroup>, published: Option<BehaviorGroup>): boolean {
    if (group && group.id && published) {
      return Boolean(this.state.publishedBehaviorGroupDiffers[group.id]);
    } else {
      return false;
    }
  }

  publishedGroupWasImported(group: BehaviorGroup): boolean {
    return Boolean(
      group.exportId &&
      BehaviorGroup.groupsIncludeExportId(this.getLocalBehaviorGroups(), group.exportId)
    );
  }

  hasRecentlyInstalledBehaviorGroups(): boolean {
    return this.getLocalBehaviorGroupsJustInstalled().length > 0;
  }

  getActivePanelName(): Option<string> {
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
    const callback = () => {
      this.props.onBehaviorGroupUpdate(existingGroup, updatedData, (newGroup: BehaviorGroup) => {
        const publishedData = this.publishedGroupDataFor(newGroup);
        if (newGroup.id && publishedData) {
          this.setState({
            publishedBehaviorGroupDiffers: this.getUpdatedPublishedGroupDiffersState(newGroup.id, this.hasDiffForPublished(newGroup, publishedData))
          });
        }
      });
    };
    if (existingGroup.id && this.isGroupChecked(existingGroup)) {
      this.onGroupCheckboxChange(existingGroup.id, false, callback);
    } else {
      callback();
    }
  }

  isImporting(group: Option<BehaviorGroup>): boolean {
    return Boolean(group && group.exportId && BehaviorGroup.groupsIncludeExportId(this.props.currentlyInstalling, group.exportId));
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
        let publishedGroupDiffersState: Option<PublishedBehaviorGroupDiffers>;
        const publishedGroup = this.publishedGroupDataFor(group);
        if (group.id && publishedGroup) {
          let publishedGroupDiffers = this.state.publishedBehaviorGroupDiffers[group.id];
          if (typeof publishedGroupDiffers === "undefined") {
            publishedGroupDiffers = this.hasDiffForPublished(group, publishedGroup);
            publishedGroupDiffersState = this.getUpdatedPublishedGroupDiffersState(group.id, publishedGroupDiffers);
          }
        }
        this.setState({
          selectedBehaviorGroup: group,
          publishedBehaviorGroupDiffers: publishedGroupDiffersState || this.state.publishedBehaviorGroupDiffers
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

  renderInstalledGroupActions(group: BehaviorGroup) {
    const groupId = group.id;
    if (!groupId) {
      return null;
    }
    const onCheckedChangeForGroup = (isChecked) => {
      this.onGroupCheckboxChange(groupId, isChecked);
    };
    return (
      <div className="columns columns-elastic type-s">
        <div className="column column-shrink">
          <div className="display-nowrap">
            <Checkbox
              className="display-block"
              onChange={onCheckedChangeForGroup}
              checked={this.isGroupChecked(group)}
              label={(
                <span className="narrow-display-none">Select</span>
              )}
            />
          </div>
        </div>
        <div className="column column-expand align-r">
          <a href={jsRoutes.controllers.BehaviorEditorController.edit(groupId).url}>Edit</a>
        </div>
      </div>
    );
  }

  renderInstalledBehaviorGroups(groups: Array<BehaviorGroup>, hasLocalGroups: boolean) {
    let heading;
    if (this.isSearching()) {
      if (this.state.onlyShowManaged) {
        heading = `Your managed skills matching “${this.getSearchText()}”`;
      } else {
        heading = `Your team’s skills matching “${this.getSearchText()}”`;
      }
    } else {
      if (this.state.onlyShowManaged) {
        heading = `Skills managed by Ellipsis`;
      } else {
        heading = `Your team’s skills`;
      }
    }
    return (
      <Collapsible revealWhen={hasLocalGroups} animationDuration={0.5}>
        <div className="container container-c ptxl">

          <div ref={(el) => this.localHeading = el}>
            <ListHeading
              heading={heading}
            />
          </div>

          <div className={"columns mvxl " + (this.isLoadingMatchingResults() ? "pulse-faded" : "")}>
            {groups.length > 0 ? groups.map((group, index) => (
              <ResponsiveColumn key={group.id || `group${index}`}>
                <BehaviorGroupCard
                  group={group}
                  onMoreInfoClick={this.toggleInfoPanel}
                  isImporting={this.isImporting(group)}
                  wasReimported={this.wasReimported(group)}
                  cardClassName="bg-white border-light"
                  secondaryActions={this.renderInstalledGroupActions(group)}
                  searchText={this.getSearchText()}
                />
              </ResponsiveColumn>
            )) : (
              <div className="mhl">
                <p>No matches</p>
              </div>
            )}
          </div>

        </div>
        <hr className="mvn rule-faint"/>
      </Collapsible>
    );
  }

  renderActions() {
    var selectedCount = this.getCheckedGroupIds().length;
    return (
      <div>
        <Button
          className="button-primary mrs mbs"
          onClick={this.clearCheckedGroups}
        >
          Cancel
        </Button>
        <Button
          className="mrs mbs"
          onClick={this.confirmDeleteBehaviorGroups}
          disabled={selectedCount < 1}
        >
          {this.getLabelForDeleteAction(selectedCount)}
        </Button>
        <Button
          className="mrl mbs"
          onClick={this.confirmMergeBehaviorGroups}
          disabled={selectedCount < 2}
        >
          Merge skills
        </Button>
        <div className="align-button mrs mbs type-italic type-weak">
          {this.getActionsLabel(selectedCount)}
        </div>
      </div>
    );
  }

  renderPublishedIntro() {
    if (this.getLocalBehaviorGroups().length === 0) {
      return (
        <div>
          <ListHeading
            heading={"To get started, install one of the skills available"}
          />

          <p className="type-blue-faded mhl mbxl">
            Each skill instructs your bot how to perform a set of related tasks, and when to respond to people in chat.
          </p>
        </div>
      );
    } else {
      return (
        <div ref={(el) => this.publishedHeading = el}>
          <ListHeading heading={this.isSearching() ?
            `Skills available to install matching “${this.getSearchText()}”` :
            "Skills available to install"
          } />
        </div>
      );
    }
  }

  renderInstallActionsFor(publishedGroup: BehaviorGroup) {
    const onImportforGroup = () => {
      this.onBehaviorGroupImport(publishedGroup);
    };
    if (this.isImporting(publishedGroup)) {
      return (
        <Button title="Installing, please wait…" className="button-raw button-no-wrap height-xl" disabled={true} onClick={null}>
          <span className="display-inline-block align-m mrs" style={{width: 40, height: 24}}><SVGInstalling /></span>
          <span className="display-inline-block align-m">Installing…</span>
        </Button>
      );
    } else if (this.publishedGroupWasImported(publishedGroup)) {
      return (
        <Button title="Already installed" className="button-raw button-no-wrap height-xl" disabled={true} onClick={null}>
          <span className="display-inline-block align-m mrs" style={{width: 40, height: 24}}><SVGInstalled /></span>
          <span className="display-inline-block align-m type-green">Installed</span>
        </Button>
      );
    } else {
      return (
        <Button title="Install this skill" className="button-raw button-no-wrap height-xl" onClick={onImportforGroup}>
          <span className="display-inline-block align-m mrs" style={{width: 40, height: 24}}><SVGInstall /></span>
          <span className="display-inline-block align-m">Install</span>
        </Button>
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

          <div className={"columns mvxl " + (this.isLoadingMatchingResults() ? "pulse-faded" : "")}>
            {groups.length > 0 ? groups.map((group) => (
              <ResponsiveColumn key={group.exportId}>
                <BehaviorGroupCard
                  group={group}
                  onMoreInfoClick={this.toggleInfoPanel}
                  isImporting={this.isImporting(group)}
                  cardClassName="bg-blue-lightest border-blue-light"
                  secondaryActions={this.renderInstallActionsFor(group)}
                  searchText={this.getSearchText()}
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

          <Button onClick={this.props.onLoadPublishedBehaviorGroups}>Try again…</Button>
        </div>
      );
    }
  }

  renderIntro() {
    return (
      <Collapsible revealWhen={this.getLocalBehaviorGroups().length === 0}>
        <div className="bg-lightest pts pbxl container container-c">
          <div className="phl">
            <div className="type-l type-light">
              This is a customizable bot that helps your team be more productive.
              Teach your bot to perform tasks and provide answers to your team.
            </div>
          </div>
        </div>
      </Collapsible>
    );
  }

  renderSearch() {
    return (
      <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
        <div className="column column-page-sidebar flex-column flex-column-left bg-lightest" />
        <div className="column column-page-main column-page-main-wide flex-column flex-column-main">
          <div className="bg-lightest-translucent pvl container container-c">
            <div className="mhl">
              <div className="columns columns-elastic mobile-columns-float">
                <div className="column column-expand mobile-mbm">
                  <SearchInput
                    placeholder="Search skills…"
                    value={this.state.userSearchText}
                    onChange={this.updateUserSearch}
                    isSearching={this.isLoadingMatchingResults()}
                    className="form-input-borderless form-input-s"
                  />
                </div>
                <div className="column column-shrink">
                  <div className="display-nowrap type-s">
                    <ToggleGroup className="form-toggle-group-s">
                      <ToggleGroupItem
                        activeWhen={!this.state.onlyShowManaged}
                        label="All skills"
                        onClick={this.showAllSkills}
                      />
                      <ToggleGroupItem
                        activeWhen={this.state.onlyShowManaged}
                        label="Managed skills"
                        onClick={this.showManagedSkills}
                      />
                    </ToggleGroup>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  renderSidebar(hasLocalGroups: boolean, hasUninstalledGroups: boolean) {
    return (
      <Sticky
        onGetCoordinates={this.getSidebarCoordinates}
        disabledWhen={() => window.innerWidth <= MOBILE_MAX_WIDTH}
      >
        <div className="pbxxl mobile-display-none">
          <div>
            <ul className="list-nav phxl mobile-phl">
              <li className={this.isScrolledToLocal() ? "list-nav-active-item" : ""}>
                <Button
                  className="button-block type-link"
                  onClick={this.scrollToLocal}
                  disabled={!hasLocalGroups}
                >
                  Your team’s skills
                </Button>
              </li>

              <li className={this.isScrolledToPublished() ? "list-nav-active-item" : ""}>
                <Button
                  className="button-block type-link"
                  onClick={this.scrollToPublished}
                  disabled={!hasUninstalledGroups}
                >
                  Skills available to install
                </Button>
              </li>
            </ul>
          </div>
        </div>
      </Sticky>
    );
  }

  renderSelectedInfoPanel() {
    const group = this.getSelectedBehaviorGroup();
    const publishedGroup = this.publishedGroupDataFor(group);
    return (
      <BehaviorGroupInfoPanel
        groupData={group}
        onToggle={this.clearActivePanel}
        isImportable={this.behaviorGroupIsUninstalled(group)}
        publishedGroupData={publishedGroup}
        publishedGroupDiffers={this.publishedGroupDataDiffersFor(group, publishedGroup)}
        isImporting={this.isImporting(group)}
        localId={group ? group.id : null}
        onBehaviorGroupImport={this.onBehaviorGroupImport}
        onBehaviorGroupUpdate={this.onBehaviorGroupUpdate}
      />
    );
  }

  render() {
    const allLocal = this.getLocalBehaviorGroups();
    const hasLocalGroups = allLocal.length > 0;
    const localGroups = this.getMatchingBehaviorGroupsFrom(allLocal);
    const filteredLocalGroups = this.state.onlyShowManaged ? localGroups.filter(ea => ea.isManaged) : localGroups;
    const allUninstalled = this.getUninstalledBehaviorGroups();
    const uninstalledGroups = this.getMatchingBehaviorGroupsFrom(allUninstalled);
    const hasUninstalledGroups = allUninstalled.length > 0;
    return (
      <div className="flex-row-cascade">
        <FixedHeader onHeightChange={this.updateSearchHeaderHeight} marginTop={this.getMainHeaderHeight()} zIndexClassName="position-z-behind-scrim">
          {this.props.notification}
          {this.renderSearch()}
        </FixedHeader>
        <div className="flex-row-cascade" style={this.getScrollableContainerStyle()}>
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide phn">
              <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
                <div className="column column-page-sidebar flex-column flex-column-left bg-lightest mobile-border-bottom prn">
                  {this.renderSidebar(hasLocalGroups, hasUninstalledGroups)}
                </div>
                <div className="column column-page-main column-page-main-wide flex-column flex-column-main">
                  {this.renderIntro()}

                  <div ref={(el) => this.localGroupContainer = el} className="bg-white">
                    {this.renderInstalledBehaviorGroups(filteredLocalGroups, hasLocalGroups)}
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
              revealWhen={this.getActivePanelName() === 'importFromGithub'}
              animationDuration={this.getAnimationDuration()}
            >
              <ImportFromGithubPanel
                isActive={this.getActivePanelName() === 'importFromGithub'}
                teamId={this.props.teamId}
                csrfToken={this.props.csrfToken}
                onDone={this.props.onClearActivePanel}
                onBehaviorGroupImport={this.onBehaviorGroupImport}
                onIsImportingToTeam={this.isImporting}
              />
            </Collapsible>
            <Collapsible
              revealWhen={this.getActivePanelName() === 'moreInfo'}
              animationDuration={this.getAnimationDuration()}
            >
              {this.renderSelectedInfoPanel()}
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
