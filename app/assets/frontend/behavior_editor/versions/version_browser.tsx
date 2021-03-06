import * as React from 'react';
import {Timestamp} from '../../lib/formatter';
import BehaviorGroup, {BehaviorGroupJson} from '../../models/behavior_group';
import BehaviorGroupDiff from './behavior_group_diff';
import Button from '../../form/button';
import Collapsible from '../../shared_ui/collapsible';
import DynamicLabelButton from '../../form/dynamic_label_button';
import FixedFooter from '../../shared_ui/fixed_footer';
import Formatter from '../../lib/formatter';
import FormInput from '../../form/input';
import GithubErrorNotification from '../github/github_error_notification';
import GithubPushPanel, {LastSavedInfo} from '../github/github_push_panel';
import LinkGithubRepo from './link_github_repo';
import LinkedGithubRepo from '../../models/linked_github_repo';
import Select from '../../form/select';
import {maybeDiffFor, ModifiedDiff} from '../../models/diffs';
import autobind from '../../lib/autobind';
import SVGWarning from '../../svg/warning';
import {GithubFetchError} from '../../models/github/github_fetch_error';
import {UpdateFromGithubSuccessData} from "../loader";
import GithubRepoActions from "./github_repo_actions";

const versionSources = {
  local: "local",
  github: "github"
};

type Props = {
  csrfToken: string,
  currentGroup: BehaviorGroup,
  currentGroupIsModified: boolean,
  currentGroupTimestamp: Option<Timestamp>,
  currentUserId: string,
  currentSelectedId?: Option<string>,
  versions: Array<BehaviorGroup>,
  onClearActivePanel: () => void,
  onUndoChanges: () => void,
  onRestoreVersionClick: (version: BehaviorGroup, title: any) => void,
  isLinkedToGithub: boolean,
  linkedGithubRepo: Option<LinkedGithubRepo>,
  onLinkGithubRepo: (owner: string, repo: string, branch: Option<string>, callback: () => void) => void,
  onUpdateFromGithub: (owner: string, repo: string, branch: string, callback: (json: UpdateFromGithubSuccessData) => void, onError: (branch: string, error?: Option<GithubFetchError>) => void) => void,
  onSaveChanges: () => void,
  isModifyingGithubRepo: boolean,
  onChangedGithubRepo: () => void
};

type State = {
  selectedMenuItem: string,
  footerHeight: number,
  branch: string,
  isChangingBranchName: boolean,
  isFetching: boolean,
  lastFetched: Option<Date>,
  isNewBranch: boolean,
  githubVersion: Option<BehaviorGroup>,
  isCommitting: boolean,
  error: Option<string>,
  lastSavedInfo: Option<LastSavedInfo>
}

type GroupedVersion = {
  key: string,
  label: string
}

type VersionGroup = {
  label?: string,
  versions: Array<GroupedVersion>
}

class VersionBrowser extends React.Component<Props, State> {
  props: Props;
  state: State;
  pushPanel: Option<GithubPushPanel>;
  linkGitHubRepoComponent: Option<LinkGithubRepo>;
  newBranchInput: Option<FormInput>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      selectedMenuItem: this.getDefaultSelectedItem(props),
      footerHeight: 0,
      branch: props.linkedGithubRepo && props.linkedGithubRepo.currentBranch ? props.linkedGithubRepo.currentBranch : "master",
      isFetching: false,
      isChangingBranchName: false,
      lastFetched: null,
      isNewBranch: false,
      githubVersion: null,
      isCommitting: false,
      error: null,
      lastSavedInfo: null
    };
  }

  onPushBranch(lastSavedInfo: LastSavedInfo): void {
    this.setState({
      githubVersion: this.props.currentGroup,
      lastFetched: new Date(),
      isNewBranch: false,
      isCommitting: false,
      lastSavedInfo: lastSavedInfo
    });
  }

  onDone(): void {
    this.props.onClearActivePanel();
    this.setState({
      lastSavedInfo: null
    });
  }

  toggleCommitting(): void {
    this.setState({
      isCommitting: !this.state.isCommitting
    }, () => {
      if (this.state.isCommitting && this.pushPanel) {
        this.pushPanel.focus();
      }
    });
  }

  getCurrentBranch(): string {
    return this.state.branch;
  }

  getSavedBranch(): string {
    const linkedGithubRepo = this.getLinkedGithubRepo();
    return linkedGithubRepo && linkedGithubRepo.currentBranch ? linkedGithubRepo.currentBranch : "master";
  }

  onBranchChange(branch: string): void {
    this.setState({
      branch: Formatter.formatGitBranchIdentifier(branch)
    });
  }

  onBranchEnterKey(): void {
    if (this.getCurrentBranch()) {
      this.doBranchChange();
    }
  }

  onUpdateFromGithub(): void {
    const linked = this.getLinkedGithubRepo();
    if (linked) {
      const owner = linked.getOwner();
      const repo = linked.getRepo();
      const branch = this.getCurrentBranch();
      this.setState({
        isFetching: true,
        error: null
      }, () => {
        this.props.onUpdateFromGithub(owner, repo, branch, this.onUpdated, this.onError);
      });
    }
  }

  onUpdated(json: { data: BehaviorGroupJson }): void {
    this.setState({
      isFetching: false,
      lastFetched: new Date(),
      isNewBranch: false,
      githubVersion: BehaviorGroup.fromJson(json.data)
    });
  }

  onError(branch: string, error?: Option<GithubFetchError>): void {
    if (error && error.type && error.type === "NoBranchFound") {
      this.setState({
        isFetching: false,
        lastFetched: null,
        isNewBranch: true,
        githubVersion: null
      });
    } else {
      this.setState({
        isFetching: false,
        error: error ? `Error: ${error.message}` : `An error occurred while retrieving “${branch}” from GitHub`
      });
    }
  }

  getDefaultSelectedItem(props: Props): string {
    if (!props.versions.length) {
      return "loading";
    } else if (props.currentGroupIsModified) {
      return "version0";
    } else if (props.linkedGithubRepo && props.isLinkedToGithub) {
      return versionSources.github;
    } else {
      return "select";
    }
  }

  componentWillReceiveProps(nextProps: Props): void {
    const versionsChanged = nextProps.versions.length !== this.props.versions.length;
    const currentGroupChanged = nextProps.currentGroup !== this.props.currentGroup &&
      !nextProps.currentGroup.isIdenticalTo(this.props.currentGroup);
    if (versionsChanged || currentGroupChanged) {
      this.onClickMenuItem(this.getDefaultSelectedItem(nextProps));
    }
  }

  componentDidUpdate(prevProps: Props): void {
    if (this.props.isModifyingGithubRepo && !prevProps.isModifyingGithubRepo) {
      this.focusOnGithubRepo();
    }
  }

  onClickMenuItem(key: string): void {
    this.setState({
      selectedMenuItem: key
    }, () => {
      if (key === versionSources.github && !this.state.lastFetched) {
        this.onUpdateFromGithub();
      }
    });
  }

  authorForVersion(version: BehaviorGroup): string {
    const isCurrentUser = version.author && version.author.ellipsisUserId === this.props.currentUserId;
    return version.author ? `by ${isCurrentUser ? "you" : version.author.formattedName()}` : "";
  }

  shortNameForVersion(version: BehaviorGroup) {
    if (this.compareGithubVersions() && this.state.lastFetched) {
      return this.renderBranchTitle(this.getSavedBranch());
    } else {
      return this.renderLocalVersionTitle(version.createdAt);
    }
  }

  getLabelForLastSavedVersion(): string {
    return "Most recent saved version";
  }

  getLinkedGithubRepo(): Option<LinkedGithubRepo> {
    return this.props.linkedGithubRepo;
  }

  getGroupedVersions(versions: Array<BehaviorGroup>): Array<VersionGroup> {
    const groups: Array<VersionGroup> = [];
    groups.push({
      versions: [{
        label: "Select a version…",
        key: "select"
      }]
    });
    const linkedGithubRepo = this.getLinkedGithubRepo();
    if (linkedGithubRepo && this.props.isLinkedToGithub) {
      groups.push({
        label: "GitHub",
        versions: [{
          label: linkedGithubRepo.getOwnerAndRepo(),
          key: versionSources.github
        }]
      });
    }
    const mostRecentDeployedVersion = versions.find((version) => Boolean(version.deployment));
    versions.forEach((version, versionIndex) => {
      const author = version.author;
      const day = version.createdAt && Formatter.formatTimestampDate(version.createdAt);
      const prevVersion = versions[versionIndex - 1];
      const prevAuthor = prevVersion && prevVersion.author;
      const prevDay = prevVersion && prevVersion.createdAt && Formatter.formatTimestampDate(prevVersion.createdAt);
      const versionLabelTimestamp = version.createdAt ? Formatter.formatTimestampShort(version.createdAt) : "Unknown save date";
      const versionLabel = version === mostRecentDeployedVersion ? `${versionLabelTimestamp} — DEPLOYED` : versionLabelTimestamp;
      const groupedVersion = {
        label: versionLabel,
        key: `version${versionIndex}`
      };
      if (versionIndex === 0) {
        groups.push({
          label: 'Most recent saved version',
          versions: [groupedVersion]
        });
      } else if (versionIndex > 1 && author && author.isSameUser(prevAuthor) && day === prevDay) {
        const lastGroup = groups[groups.length - 1];
        lastGroup.versions.push(groupedVersion);
      } else {
        groups.push({
          label: `Saved ${day ? `on ${day}` : ""} ${this.authorForVersion(version)}`,
          versions: [groupedVersion]
        });
      }
    });
    return groups;
  }

  renderVersionGroup(versions: Array<GroupedVersion>) {
    return versions.map((groupedVersion) => (
      <option key={groupedVersion.key} value={groupedVersion.key}>{groupedVersion.label}</option>
    ));
  }

  renderVersionOptions() {
    if (this.props.versions.length > 0) {
      return this.getGroupedVersions(this.props.versions).map((versionGroup, groupIndex) => {
        if (versionGroup.label) {
          return (
            <optgroup label={versionGroup.label} key={`versionGroup${groupIndex}`}>
              {this.renderVersionGroup(versionGroup.versions)}
            </optgroup>
          );
        } else {
          return this.renderVersionGroup(versionGroup.versions);
        }
      });
    } else {
      return (
        <option className="pulse type-disabled" value="loading">Loading versions…</option>
      );
    }
  }

  compareGithubVersions(): boolean {
    return this.state.selectedMenuItem === versionSources.github;
  }

  getSelectedVersionIndex(): Option<number> {
    if (this.state.selectedMenuItem) {
      const match = this.state.selectedMenuItem.match(/version(\d+)/);
      return match ? parseInt(match[1], 10) : null;
    } else {
      return null;
    }
  }

  getSelectedVersion(): Option<BehaviorGroup> {
    const index = this.getSelectedVersionIndex();
    if (typeof index === "number") {
      return this.getVersionIndex(index);
    } else if (this.compareGithubVersions()) {
      return this.state.githubVersion;
    } else {
      return null;
    }
  }

  getVersionIndex(index: number): Option<BehaviorGroup> {
    return this.props.versions[index];
  }

  getDiffForSelectedVersion(selectedVersion: Option<BehaviorGroup>): Option<ModifiedDiff<BehaviorGroup>> {
    // original.maybeDiffFor(modified) shows changes from original to modified
    if (selectedVersion) {
      return this.selectedIsNewer() ?
        maybeDiffFor(this.props.currentGroup, selectedVersion, null, false) :
        maybeDiffFor(selectedVersion, this.props.currentGroup, null, false);
    } else {
      return null;
    }
  }

  getFooterHeight(): number {
    return this.state.footerHeight;
  }

  setFooterHeight(height: number): void {
    this.setState({
      footerHeight: height
    });
  }

  revertToSelected(): void {
    const selected = this.getSelectedVersion();
    if (selected && this.latestVersionIsSelected() && this.props.currentGroupIsModified) {
      this.props.onUndoChanges();
    } else if (selected) {
      const title = this.shortNameForVersion(selected);
      this.props.onRestoreVersionClick(selected, title);
    }
  }

  renderSelectedVersion(selectedVersion: Option<BehaviorGroup>, diff: Option<ModifiedDiff<BehaviorGroup>>) {
    if (selectedVersion) {
      return (
        <div>
          {this.renderDiff(diff)}
        </div>
      );
    } else if (this.props.versions.length === 0) {
      return (
        <div className="pulse">Loading version history…</div>
      );
    } else if (this.compareGithubVersions() && this.state.isNewBranch) {
      return (
        <div className="type-bold">This is a new branch. It has not yet been pushed to GitHub.</div>
      );
    } else if (this.compareGithubVersions() && !this.state.isNewBranch) {
      return (
        <div className="type-italic">Select a GitHub branch to compare.</div>
      );
    } else {
      return (
        <div className="type-italic">Select another version to compare.</div>
      );
    }
  }

  summarizeNoDiff() {
    if (this.state.selectedMenuItem === "select") {
      return (
        <div className="type-italic">Select a version of this skill to compare to the current version.</div>
      );
    } else if (this.getSelectedVersionIndex() === 0 && !this.props.currentGroupIsModified) {
      return (
        <div>No changes have been made since this version was saved.</div>
      );
    } else if (this.compareGithubVersions()) {
      return (
        <div>The current version is identical to the version in the {this.renderBranchTitle(this.getSavedBranch())} on
          GitHub.</div>
      );
    } else {
      return (
        <div>These two versions are identical.</div>
      );
    }
  }

  renderDiff(diff: Option<ModifiedDiff<BehaviorGroup>>) {
    if (diff) {
      return (
        <BehaviorGroupDiff diff={diff} />
      );
    } else {
      return this.summarizeNoDiff();
    }
  }

  renderSelectableVersion() {
    return (
      <div className="bg-lightest border-bottom container container-wide ptm pbs">
        <span className="align-button align-button-s align-t type-label mrs">Select version to compare:</span>
        <Select className="align-b form-select-s mrs mbs" value={this.state.selectedMenuItem}
          onChange={this.onClickMenuItem}>
          {this.renderVersionOptions()}
        </Select>
        {this.renderSelectedVersionNote()}
      </div>
    );
  }

  latestVersionIsSelected(): boolean {
    return this.state.selectedMenuItem === "version0";
  }

  renderNoteForVersion(note: string) {
    return (
      <span className="mrs type-weak">({note})</span>
    );
  }

  renderSelectedVersionNote() {
    const version = this.props.versions[0];
    if (this.latestVersionIsSelected() && version) {
      return this.renderNoteForVersion(this.getLabelForLastSavedVersion());
    } else {
      return null;
    }
  }

  renderCurrentVersionNote() {
    if (!this.props.currentGroupIsModified && this.props.currentGroupTimestamp) {
      return this.renderNoteForVersion(Formatter.formatTimestampShort(this.props.currentGroupTimestamp));
    } else {
      return null;
    }
  }

  renderCurrentVersionPlaceholder() {
    return (
      <span>
        <span className="type-bold mrs">
          {this.props.currentGroupIsModified ? "Current version (unsaved)" : "Current saved version"}
        </span>
        {this.renderCurrentVersionNote()}
      </span>
    );
  }

  renderSelectedVersionPlaceholder() {
    const selectedVersion = this.getSelectedVersion();
    const githubVersionTitle = this.props.linkedGithubRepo ? (
      <span>
        <span className="mrxs">Branch</span>
        <span className="type-monospace">{this.props.linkedGithubRepo.currentBranch}</span>
        <span className="mlxs">on GitHub</span>
      </span>
    ) : "";
    return (
      <span>
        <span className="type-bold mrs">
          {this.compareGithubVersions() ? githubVersionTitle : "Previous version"}
        </span>
        {this.renderNoteForVersion(selectedVersion && selectedVersion.createdAt ?
          Formatter.formatTimestampShort(selectedVersion.createdAt) :
          "Date unknown")}
      </span>
    )
  }

  renderSaveButton() {
    if (this.props.currentGroupIsModified) {
      return (
        <Button className="mrs mbm" onClick={this.props.onSaveChanges}>
          Save changes
        </Button>
      );
    } else {
      return null;
    }
  }

  renderRevertButton(selectedVersion: Option<BehaviorGroup>, hasChanges: boolean) {
    return (
      <Button className="mrs mbm" onClick={this.revertToSelected} disabled={!hasChanges}>
        {this.renderRevertButtonTitle(selectedVersion, hasChanges)}
      </Button>
    );
  }

  renderRevertButtonTitle(selectedVersion: Option<BehaviorGroup>, hasChanges: boolean) {
    if (this.compareGithubVersions()) {
      const branch = this.getSavedBranch();
      const selectedIsNewer = this.selectedIsNewer();
      if (selectedIsNewer) {
        return branch ? (
          <span>Update current version to {this.renderBranchTitle(branch)}…</span>
        ) : (
          <span>Pull…</span>
        );
      } else {
        return branch ? (
          <span>Revert current version to {this.renderBranchTitle(branch)}…</span>
        ) : (
          <span>Revert…</span>
        );
      }
    } else {
      if (selectedVersion && hasChanges) {
        return (
          <span>Revert to {this.shortNameForVersion(selectedVersion)}…</span>
        );
      } else {
        return (
          <span>Revert…</span>
        );
      }
    }
  }

  renderCommitButtonLabel(branchTitle: React.ReactNode, githubIsNewer: boolean) {
    if (this.state.isNewBranch) {
      return (
        <span>Create new {branchTitle} on GitHub…</span>
      );
    } else if (githubIsNewer) {
      const currentDate = this.props.currentGroupTimestamp ? Formatter.formatTimestampShort(this.props.currentGroupTimestamp) : "current version";
      return (
        <span>Revert {branchTitle} to {currentDate}…</span>
      );
    } else {
      return (
        <span>Push changes to {branchTitle} on GitHub…</span>
      );
    }
  }

  renderCommitButton(selectedVersion: Option<BehaviorGroup>, hasChanges: boolean) {
    if (this.getLinkedGithubRepo() && this.compareGithubVersions()) {
      const unsavedChanges = this.props.currentGroupIsModified;
      const githubVersionIsIdentical = Boolean(selectedVersion && !hasChanges);
      const branchTitle = this.renderBranchTitle(this.getSavedBranch());
      return (
        <Button
          onClick={this.toggleCommitting}
          disabled={unsavedChanges || githubVersionIsIdentical}
          className="mrs mbm"
        >{this.renderCommitButtonLabel(branchTitle, this.selectedIsNewer())}</Button>
      );
    } else {
      return null;
    }
  }

  onLinkedGithubRepo(): void {
    this.props.onChangedGithubRepo();
    if (this.props.isLinkedToGithub && this.getLinkedGithubRepo()) {
      this.onClickMenuItem(versionSources.github);
    }
  }

  focusOnGithubRepo(): void {
    if (this.linkGitHubRepoComponent) {
      this.linkGitHubRepoComponent.focus();
    }
  }

  isBranchUnchanged(): boolean {
    return this.getLinkedGithubRepo() ? this.getSavedBranch() === this.getCurrentBranch() : false;
  }

  changeBranch(): void {
    this.setState({
      isChangingBranchName: true
    });
  }

  cancelChangeBranch(): void {
    this.setState({
      isChangingBranchName: false,
      branch: this.getSavedBranch()
    });
  }

  doBranchChange(): void {
    this.setState({
      isChangingBranchName: false
    }, this.onUpdateFromGithub);
  }

  onBranchNameSectionToggle(isRevealed: boolean): void {
    if (isRevealed && this.newBranchInput) {
      this.newBranchInput.select();
    }
  }

  renderGithubBranch() {
    return (
      <div className="display-inline-block">
        <div className="align-button align-button-s mrs">
          <span className="type-label mrs">Branch:</span>
          <span className="type-monospace type-bold">{this.getCurrentBranch() || "(no branch)"}</span>
        </div>
        <DynamicLabelButton
          className="button-shrink button-s mrs"
          onClick={this.onUpdateFromGithub}
          disabledWhen={this.state.isFetching || !this.getCurrentBranch() || this.state.isChangingBranchName}
          labels={[{
            text: "Refresh",
            displayWhen: !this.state.isFetching
          }, {
            text: "Fetching…",
            displayWhen: this.state.isFetching
          }]}
        />
        <Button onClick={this.changeBranch} className="button-shrink button-s mrs" disabled={this.state.isChangingBranchName}>Change branch</Button>
      </div>
    );
  }

  renderGithubBranchInput() {
    return (
      <div className="position-absolute position-top-left position-top-right position-z-above">
        <Collapsible revealWhen={this.state.isChangingBranchName} onChange={this.onBranchNameSectionToggle}>
          <div className="bg-white border-bottom ptm pbs container container-wide">
            <span className="align-button align-button-s type-label mrs mbs">Enter branch name:</span>
            <FormInput
              ref={(el) => this.newBranchInput = el}
              className="form-input-borderless form-input-s type-monospace width-15 mrs mbs"
              placeholder="Branch (e.g. master)"
              onChange={this.onBranchChange}
              onEnterKey={this.onBranchEnterKey}
              value={this.getCurrentBranch()}
            />
            <DynamicLabelButton
              className="button-shrink button-s mrs mbs"
              onClick={this.doBranchChange}
              disabledWhen={this.state.isFetching || !this.getCurrentBranch() || this.isBranchUnchanged()}
              labels={[{
                text: "Select branch",
                displayWhen: !this.state.isFetching
              }, {
                text: "Updating…",
                displayWhen: this.state.isFetching
              }]}
            />
            <Button
              className="button-shrink button-s mbs"
              onClick={this.cancelChangeBranch}
            >Cancel</Button>
          </div>
        </Collapsible>
      </div>
    );
  }

  renderGithubStatus() {
    if (this.state.error) {
      return (
        <div className="mvs">
          <GithubErrorNotification error={this.state.error} />
        </div>
      );
    } else {
      return null;
    }
  }

  selectedIsNewer(): boolean {
    const selectedVersion = this.getSelectedVersion();
    const selectedVersionTimestamp = selectedVersion ? selectedVersion.createdAt : null;
    const selectedVersionDate = selectedVersionTimestamp ? new Date(selectedVersionTimestamp) : null;
    const currentVersionDate = this.props.currentGroupTimestamp ? new Date(this.props.currentGroupTimestamp) : null;
    return Boolean(selectedVersionDate && currentVersionDate && Number(selectedVersionDate) > Number(currentVersionDate));
  }

  renderVersionSelector() {
    const shouldInvert = this.selectedIsNewer();
    if (this.props.versions.length > 0) {
      return (
        <div className="type-s">
          {this.renderSelectableVersion()}
          <div className="bg-lightest border-emphasis-bottom border-pink container container-wide">
            <div className="columns">
              <div className="column column-one-half border-right mrneg1 ptl pbs">
                <span>Older: </span>
                {shouldInvert ?
                  this.renderCurrentVersionPlaceholder() :
                  this.renderSelectedVersionPlaceholder()
                }
              </div>
              <div className="column column-one-half border-left pll ptl pbs">
                <span>Newer: </span>
                {shouldInvert ?
                  this.renderSelectedVersionPlaceholder() :
                  this.renderCurrentVersionPlaceholder()
                }
              </div>
            </div>
          </div>
        </div>
      );
    } else {
      return (
        <div className="bg-lightest border-emphasis-bottom border-pink container container-wide pvl">
          <div className="align-button align-button-s pulse type-italic type-weak">Loading…</div>
        </div>
      );
    }
  }

  renderBranchTitle(branchName: string) {
    return (
      <span>
          <span className="type-monospace type-bold mhxs">{branchName}</span>
          <span> branch</span>
        </span>
    );
  }

  renderLocalVersionTitle(timestamp: Option<Timestamp>) {
    return this.getSelectedVersionIndex() === 0 ? (
      <span>last saved version</span>
    ) : (
      <span>version {timestamp ? `dated ${Formatter.formatTimestampShort(timestamp)}` : "with unknown date"}</span>
    );
  }

  getContainerStyle(): React.CSSProperties {
    return {
      paddingBottom: `${this.getFooterHeight()}px`
    };
  }

  clearLastSavedInfo() {
    this.setState({
      lastSavedInfo: null
    });
  }

  renderLastSavedInfo() {
    const lastSavedInfo = this.state.lastSavedInfo;
    if (lastSavedInfo) {
      return (
        <div className={`type-s mbneg1 container container-wide ${
            lastSavedInfo.noCommit ? "box-warning" : "box-tip"
          }`}>
          <span className="mrm">
            {lastSavedInfo.noCommit ? (
              <span>
                <span className="display-inline-block height-xl mrs type-yellow align-m"><SVGWarning /></span>
                <span>Branch <b>{lastSavedInfo.branch}</b> created, but no changes committed because it is identical to <b>master</b>.</span>
              </span>
            ) : (
              <span> Branch <b>{lastSavedInfo.branch}</b> pushed {Formatter.formatTimestampRelative(lastSavedInfo.date)}.</span>
            )}
          </span>
          <Button onClick={this.clearLastSavedInfo} className="button-s button-shrink">OK</Button>
        </div>
      );
    } else {
      return (
        <div/>
      );
    }
  }

  render() {
    const selectedVersion = this.getSelectedVersion();
    const diff = this.getDiffForSelectedVersion(selectedVersion);
    const hasChanges = Boolean(diff);
    const hasGithubRepo = Boolean(this.props.linkedGithubRepo);
    return (
      <div className="flex-row-cascade" style={this.getContainerStyle()}>

        <div className="flex-columns flex-row-expand">
          <div className="flex-column flex-column-left flex-rows bg-white">

            <Collapsible revealWhen={this.props.isModifyingGithubRepo}>
              <div className="bg-white border-emphasis-bottom border-light container container-wide pvl">
                <LinkGithubRepo
                  ref={(el) => this.linkGitHubRepoComponent = el}
                  linked={this.getLinkedGithubRepo()}
                  onDoneClick={this.onLinkedGithubRepo}
                  onLinkGithubRepo={this.props.onLinkGithubRepo}
                />
              </div>
            </Collapsible>

            <div className={`bg-white pvm container container-wide border-bottom ${this.state.isChangingBranchName ? "border-light" : ""}`}>
              <div className="display-inline-block mrxl">
                <GithubRepoActions
                  linkedGithubRepo={this.props.linkedGithubRepo}
                  isLinkedToGithub={this.props.isLinkedToGithub}
                  currentGroupIsModified={this.props.currentGroupIsModified}
                  onChangeGithubRepo={this.props.onChangedGithubRepo}
                  currentGroup={this.props.currentGroup}
                  currentSelectedId={this.props.currentSelectedId}
                />
              </div>
              {hasGithubRepo ? (
                <div className="display-inline-block">
                  {this.renderGithubBranch()}
                </div>
              ) : null}
              <div>
                {this.renderGithubStatus()}
              </div>
            </div>

            <div className="position-relative">
              {hasGithubRepo ? this.renderGithubBranchInput() : null}
            </div>

            {this.renderVersionSelector()}

            <div className="container container container-wide pvxl">
              {this.renderSelectedVersion(selectedVersion, diff)}
            </div>
          </div>
        </div>

        <FixedFooter onHeightChange={this.setFooterHeight}>
          <Collapsible revealWhen={Boolean(this.state.lastSavedInfo)}>
            {this.renderLastSavedInfo()}
          </Collapsible>
          <Collapsible revealWhen={!this.state.isCommitting}>
            <div className="ptm bg-lightest border-top container container-wide">
              <Button className="mrs mbm button-primary" onClick={this.onDone}>Done</Button>
              {this.renderSaveButton()}
              {this.renderRevertButton(selectedVersion, hasChanges)}
              {this.renderCommitButton(selectedVersion, hasChanges)}
            </div>
          </Collapsible>
          <Collapsible revealWhen={this.state.isCommitting}>
            <GithubPushPanel
              ref={(el) => this.pushPanel = el}
              group={this.props.currentGroup}
              linked={this.getLinkedGithubRepo()}
              onPushBranch={this.onPushBranch}
              onDoneClick={this.toggleCommitting}
              csrfToken={this.props.csrfToken}
              localIsOlder={this.selectedIsNewer()}
            />
          </Collapsible>
        </FixedFooter>
      </div>
    );
  }
}

export default VersionBrowser;

