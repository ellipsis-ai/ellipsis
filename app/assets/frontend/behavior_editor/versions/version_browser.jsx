// @flow
import * as React from 'react';
import type {Timestamp} from '../../lib/formatter';
import BehaviorGroup from '../../models/behavior_group';
import BehaviorGroupDiff from './behavior_group_diff';
import Button from '../../form/button';
import Collapsible from '../../shared_ui/collapsible';
import DynamicLabelButton from '../../form/dynamic_label_button';
import FixedHeader from '../../shared_ui/fixed_header';
import FixedFooter from '../../shared_ui/fixed_footer';
import Formatter from '../../lib/formatter';
import FormInput from '../../form/input';
import GithubErrorNotification from '../github/github_error_notification';
import GithubOwnerRepoReadonly from '../github/github_owner_repo_readonly';
import GithubPushPanel from '../github/github_push_panel';
import LinkGithubRepo from './link_github_repo';
import LinkedGithubRepo from '../../models/linked_github_repo';
import Select from '../../form/select';
import {maybeDiffFor, ModifiedDiff} from '../../models/diffs';
import autobind from '../../lib/autobind';

const versionSources = {
  local: "local",
  github: "github"
};

type GithubFetchError = {
  message: string,
  type?: string,
  details?: {}
}

type Props = {
    csrfToken: string,
    currentGroup: BehaviorGroup,
    currentGroupIsModified: boolean,
    currentUserId: string,
    currentSelectedId?: string,
    versions: Array<BehaviorGroup>,
    onClearActivePanel: () => void,
    onUndoChanges: () => void,
    onRestoreVersionClick: (version: BehaviorGroup, title: React.Node) => void,
    isLinkedToGithub: boolean,
    linkedGithubRepo?: LinkedGithubRepo,
    onLinkGithubRepo: (owner: string, repo: string, branch: ?string, callback: () => void) => void,
    onUpdateFromGithub: (owner: string, repo: string, branch: string, callback: ({ data: {} }) => void, onError: (branch: string, error?: GithubFetchError) => void) => void,
    onSaveChanges: () => void
};

type State = {
    selectedMenuItem: string,
    diffFromSelectedToCurrent: boolean,
    headerHeight: number,
    footerHeight: number,
    isModifyingGithubRepo: boolean,
    branch: string,
    isChangingBranchName: boolean,
    isFetching: boolean,
    lastFetched: ?Date,
    isNewBranch: boolean,
    githubVersion: ?BehaviorGroup,
    isCommitting: boolean,
    error: ?string
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
    pushPanel: ?GithubPushPanel;
    linkGitHubRepoComponent: ?LinkGithubRepo;
    newBranchInput: ?FormInput;

    constructor(props: Props): void {
      super(props);
      autobind(this);
      this.state = {
        selectedMenuItem: this.getDefaultSelectedItem(props),
        diffFromSelectedToCurrent: true,
        headerHeight: 0,
        footerHeight: 0,
        isModifyingGithubRepo: false,
        branch: props.linkedGithubRepo && props.linkedGithubRepo.currentBranch ? props.linkedGithubRepo.currentBranch : "master",
        isFetching: false,
        isChangingBranchName: false,
        lastFetched: null,
        isNewBranch: false,
        githubVersion: null,
        isCommitting: false,
        error: null
      };
    }

    onPushBranch(): void {
      const newState = {};
      newState.githubVersion = this.props.currentGroup;
      newState.lastFetched = new Date();
      if (this.state.isNewBranch) {
        newState.isNewBranch = false;
      }
      this.setState(newState);
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
      return this.props.linkedGithubRepo && this.props.linkedGithubRepo.currentBranch ? this.props.linkedGithubRepo.currentBranch : "master";
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
      if (this.props.linkedGithubRepo) {
        const linked = this.props.linkedGithubRepo;
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

    onUpdated(json: { data: {} }): void {
      this.setState({
        isFetching: false,
        lastFetched: new Date(),
        isNewBranch: false,
        githubVersion: BehaviorGroup.fromJson(json.data)
      });
    }

    onError(branch: string, error?: GithubFetchError): void {
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
      } else {
        return "select";
      }
    }

    componentWillReceiveProps(nextProps: Props): void {
      const versionsChanged = nextProps.versions.length !== this.props.versions.length;
      const currentGroupChanged = nextProps.currentGroup !== this.props.currentGroup &&
        !nextProps.currentGroup.isIdenticalTo(this.props.currentGroup);
      if (versionsChanged || currentGroupChanged) {
        this.setState({
          selectedMenuItem: this.getDefaultSelectedItem(nextProps)
        });
      }
    }

    getGithubAuthUrl(): string {
      const redirect = jsRoutes.controllers.BehaviorEditorController.edit(this.props.currentGroup.id, this.props.currentSelectedId, true).url;
      return jsRoutes.controllers.SocialAuthController.authenticateGithub(redirect).url;
    }

    renderGithubAuth(): React.Node {
      return (
        <span className="type-s">
          <a href={this.getGithubAuthUrl()}>
            <img height="24" src="/assets/images/logos/GitHub-Mark-64px.png" className="mrs align-m" />
            <span>Authenticate with GitHub</span>
          </a>
          <span> to sync this skill with a GitHub repo</span>
        </span>
      );
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
      const isCurrentUser = version.author && version.author.id === this.props.currentUserId;
      return version.author ? `by ${isCurrentUser ? "you" : version.author.formattedName()}` : "";
    }

    shortNameForVersion(version: BehaviorGroup): React.Node {
      if (this.compareGithubVersions() && this.state.lastFetched) {
        return this.renderBranchTitle(this.getSavedBranch());
      } else {
        return this.renderLocalVersionTitle(version.createdAt);
      }
    }

    getLabelForLastSavedVersion(): string {
      return "Most recent saved version";
    }

    getGroupedVersions(versions: Array<BehaviorGroup>): Array<VersionGroup> {
      const groups: Array<VersionGroup> = [];
      groups.push({
        versions: [{
          label: "Select a version…",
          key: "select"
        }]
      });
      if (this.props.linkedGithubRepo && this.props.isLinkedToGithub) {
        groups.push({
          label: "GitHub",
          versions: [{
            label: this.props.linkedGithubRepo.getOwnerAndRepo(),
            key: versionSources.github
          }]
        });
      }
      versions.forEach((version, versionIndex) => {
        const author = version.author;
        const day = version.createdAt && Formatter.formatTimestampDate(version.createdAt);
        const prevVersion = versions[versionIndex - 1];
        const prevAuthor = prevVersion && prevVersion.author;
        const prevDay = prevVersion && prevVersion.createdAt && Formatter.formatTimestampDate(prevVersion.createdAt);
        const versionLabel = version.createdAt ? Formatter.formatTimestampShort(version.createdAt) : "Unknown save date";
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
            label: `Saved ${day ? `on ${day}`: ""} ${this.authorForVersion(version)}`,
            versions: [groupedVersion]
          });
        }
      });
      return groups;
    }

    renderVersionGroup(versions: Array<GroupedVersion>): React.Node {
      return versions.map((groupedVersion) => (
        <option key={groupedVersion.key} value={groupedVersion.key}>{groupedVersion.label}</option>
      ));
    }

    renderVersionOptions(): React.Node {
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

    getSelectedVersionIndex(): ?number {
      if (this.state.selectedMenuItem) {
        const match = this.state.selectedMenuItem.match(/version(\d+)/);
        return match ? parseInt(match[1], 10) : null;
      } else {
        return null;
      }
    }

    getSelectedVersion(): ?BehaviorGroup {
      const index = this.getSelectedVersionIndex();
      if (typeof index === "number") {
        return this.getVersionIndex(index);
      } else if (this.compareGithubVersions()) {
        return this.state.githubVersion;
      } else {
        return null;
      }
    }

    getVersionIndex(index: number): ?BehaviorGroup {
      return this.props.versions[index];
    }

    invertDiffDirection(): void {
      this.setState({
        diffFromSelectedToCurrent: !this.state.diffFromSelectedToCurrent
      });
    }

    getDiffForSelectedVersion(selectedVersion: ?BehaviorGroup): ?ModifiedDiff<BehaviorGroup> {
      // original.maybeDiffFor(modified) shows changes from original to modified
      if (selectedVersion) {
        return this.state.diffFromSelectedToCurrent ?
          maybeDiffFor(selectedVersion, this.props.currentGroup) :
          maybeDiffFor(this.props.currentGroup, selectedVersion);
      } else {
        return null;
      }
    }

    getHeaderHeight(): number {
      return this.state.headerHeight;
    }

    getFooterHeight(): number {
      return this.state.footerHeight;
    }

    setHeaderHeight(height: number): void {
      this.setState({
        headerHeight: height
      });
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

    renderSelectedVersion(selectedVersion: ?BehaviorGroup, diff: ?ModifiedDiff<BehaviorGroup>): React.Node {
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

    summarizeNoDiff(): React.Node {
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
          <div>The current version is identical to the version in the {this.renderBranchTitle(this.getSavedBranch())} on GitHub.</div>
        );
      } else {
        return (
          <div>These two versions are identical.</div>
        );
      }
    }

    renderDiff(diff: ?ModifiedDiff<BehaviorGroup>): React.Node {
      if (diff) {
        return (
          <BehaviorGroupDiff diff={diff} />
        );
      } else {
        return this.summarizeNoDiff();
      }
    }

    renderSelectableVersion(caption: React.Node): React.Node {
      const githubMode = this.compareGithubVersions();
      return (
        <div>
          <Collapsible revealWhen={!this.state.isChangingBranchName}>
            {caption}
            <Select className="align-b form-select-s mrs mbs" value={this.state.selectedMenuItem} onChange={this.onClickMenuItem}>
              {this.renderVersionOptions()}
            </Select>
            {this.renderSelectedVersionNote()}
            {githubMode ? this.renderGithubBranch() : null}
            {githubMode ? this.renderGithubStatus() : null}
          </Collapsible>
          {githubMode ? this.renderGithubBranchInput() : null}
        </div>
      );
    }

    latestVersionIsSelected(): boolean {
      return this.state.selectedMenuItem === "version0";
    }

    renderNoteForVersion(note: string): React.Node {
      return (
        <span className="align-button align-button-s mrs mbs type-weak">({note})</span>
      );
    }

    renderSelectedVersionNote(): React.Node {
      const version = this.props.versions[0];
      if (this.latestVersionIsSelected() && version) {
        return this.renderNoteForVersion(this.getLabelForLastSavedVersion());
      }
    }

    renderCurrentVersionNote(): React.Node {
      if (!this.props.currentGroupIsModified && this.props.currentGroup.createdAt) {
        return this.renderNoteForVersion(Formatter.formatTimestampShort(this.props.currentGroup.createdAt));
      }
    }

    renderCurrentVersionPlaceholder(caption: React.Node): React.Node {
      return (
        <div>
          {caption}
          <div className="align-button align-button-s type-bold mrs mbs">
            {this.props.currentGroupIsModified ? "Current version (unsaved)" : "Current saved version"}
          </div>
          {this.renderCurrentVersionNote()}
        </div>
      );
    }

    renderSaveButton(): React.Node {
      if (this.props.currentGroupIsModified) {
        return (
          <Button className="mrs mbm" onClick={this.props.onSaveChanges}>
            Save changes
          </Button>
        );
      }
    }

    renderRevertButton(selectedVersion: ?BehaviorGroup, hasChanges: boolean): React.Node {
      return (
        <Button className="mrs mbm" onClick={this.revertToSelected} disabled={!hasChanges}>
          {this.renderRevertButtonTitle(selectedVersion, hasChanges)}
        </Button>
      );
    }

    renderRevertButtonTitle(selectedVersion: ?BehaviorGroup, hasChanges: boolean): React.Node {
      if (this.compareGithubVersions()) {
        const branch = this.getSavedBranch();
        return branch ? (
          <span>Revert current version to {this.renderBranchTitle(branch)}…</span>
        ) : (
          <span>Revert…</span>
        );
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

    renderCommitButton(selectedVersion: ?BehaviorGroup, hasChanges: boolean): React.Node {
      if (this.props.linkedGithubRepo && this.compareGithubVersions()) {
        const unsavedChanges = this.props.currentGroupIsModified;
        const githubVersionIsIdentical = selectedVersion && !hasChanges;
        const branchTitle = this.renderBranchTitle(this.getSavedBranch());
        const label = this.state.isNewBranch ? (
          <span>Push new {branchTitle} to GitHub…</span>
        ) : (
          <span>Update {branchTitle} with current version…</span>
        );
        return (
          <Button
            onClick={this.toggleCommitting}
            disabled={unsavedChanges || githubVersionIsIdentical}
            className="mrs mbm"
          >{label}</Button>
        );
      }
    }

    onLinkedGithubRepo(): void {
      this.setState({
        isModifyingGithubRepo: false
      });
    }

    onChangeGithubLinkClick(): void {
      this.setState({
        isModifyingGithubRepo: true
      }, this.focusOnGithubRepo);
    }

    focusOnGithubRepo(): void {
      if (this.linkGitHubRepoComponent) {
        this.linkGitHubRepoComponent.focus();
      }
    }

    isBranchUnchanged(): boolean {
      return this.props.linkedGithubRepo ? this.getSavedBranch() === this.getCurrentBranch() : false;
    }

    changeBranch(): void {
      this.setState({
        isChangingBranchName: true
      }, () => {
        if (this.newBranchInput) {
          this.newBranchInput.select();
        }
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

    renderGithubBranch(): React.Node {
      return (
        <div className="display-inline-block mbs">
          <div className="align-button align-button-s mrs">
            <div className="type-monospace type-bold">{this.getCurrentBranch() || "(no branch)"}</div>
          </div>
          <DynamicLabelButton
            className="button-shrink button-s mrs"
            onClick={this.onUpdateFromGithub}
            disabledWhen={this.state.isFetching || !this.getCurrentBranch()}
            labels={[{
              text: "Refresh",
              displayWhen: !this.state.isFetching
            }, {
              text: "Fetching…",
              displayWhen: this.state.isFetching
            }]}
          />
          <Button onClick={this.changeBranch} className="button-shrink button-s mrs">Change branch</Button>
        </div>
      );
    }

    renderGithubBranchInput(): React.Node {
      return (
        <Collapsible revealWhen={this.state.isChangingBranchName}>
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
        </Collapsible>
      );
    }

    renderGithubStatus(): React.Node {
      if (this.state.error) {
        return (
          <div className="align-button align-button-s mbs">
            <GithubErrorNotification error={this.state.error}/>
          </div>
        );
      }
    }

    renderLeftCaption(): React.Node {
      return (
        <div className="align-button align-button-s mrs mbs">Compare</div>
      );
    }

    renderRightCaption(): React.Node {
      return (
        <div className="align-button align-button-s mrs mbs">to</div>
      );
    }

    renderVersionSelector(hasChanges: boolean): React.Node {
      if (this.props.versions.length > 0) {
        return (
          <div>
            <div className="columns type-s">
              <div className="column column-one-half border-right mrneg1 ptl pbs">
                <div>
                  {this.state.diffFromSelectedToCurrent ?
                    this.renderSelectableVersion(this.renderLeftCaption()) :
                    this.renderCurrentVersionPlaceholder(this.renderLeftCaption())
                  }
                </div>
              </div>
              <div className="column column-one-half border-left pll ptl pbs">
                <div className="columns columns-elastic">
                  <div className="column column-expand">
                    {this.state.diffFromSelectedToCurrent ?
                      this.renderCurrentVersionPlaceholder(this.renderRightCaption()) :
                      this.renderSelectableVersion(this.renderRightCaption())
                    }
                  </div>
                  <div className="column column-shrink">
                    {hasChanges ? (
                      <Button onClick={this.invertDiffDirection} className="button-s button-shrink" title="Invert the direction changes are shown">⇄</Button>
                    ) : null}
                  </div>
                </div>
              </div>
            </div>
          </div>
        );
      } else {
        return (
          <div className="pvl">
            <div className="align-button align-button-s pulse type-italic type-weak">Loading…</div>
          </div>
        );
      }
    }

    renderBranchTitle(branchName: string): React.Node {
      return (
        <span>
          <span className="type-monospace type-bold mhxs">{branchName}</span>
          <span> branch</span>
        </span>
      );
    }

    renderLocalVersionTitle(timestamp: ?Timestamp): React.Node {
      return this.getSelectedVersionIndex() === 0 ? (
        <span>last saved version</span>
      ) : (
        <span>version {timestamp ? `dated ${Formatter.formatTimestampShort(timestamp)}` : "with unknown date"}</span>
      );
    }

    renderGithubRepo(): React.Node {
      if (this.props.linkedGithubRepo && this.props.isLinkedToGithub) {
        return (
          <div>
            <span className="mrm">
              <span className="type-label mrs">GitHub repository:</span>
              <GithubOwnerRepoReadonly linked={this.props.linkedGithubRepo}/>
            </span>
            {this.renderChangeRepoButton()}
          </div>
        );
      } else if (!this.props.isLinkedToGithub && !this.props.currentGroupIsModified) {
        return this.renderGithubAuth();
      } else if (!this.props.linkedGithubRepo) {
        return this.renderChangeRepoButton();
      }
    }

    renderChangeRepoButton(): React.Node {
      return (
        <Button className="button-s button-shrink" onClick={this.onChangeGithubLinkClick} disabled={this.state.isModifyingGithubRepo}>
          {this.props.linkedGithubRepo ? "Change repo…" : "Link GitHub repo…"}
        </Button>
      );
    }

    getMainHeaderHeight(): number {
      const mainHeader = document.getElementById("main-header");
      return mainHeader ? mainHeader.offsetHeight : 0;
    }

    getContainerStyle() {
      const myHeaderHeight = this.getHeaderHeight();
      const mainHeaderHeight = this.getMainHeaderHeight();
      const headerHeight = Math.max(0, myHeaderHeight - mainHeaderHeight);
      const footerHeight = this.getFooterHeight();
      return {
        paddingTop: `${headerHeight}px`,
        paddingBottom: `${footerHeight}px`
      };
    }

    render(): React.Node {
      const selectedVersion = this.getSelectedVersion();
      const diff = this.getDiffForSelectedVersion(selectedVersion);
      const hasChanges = Boolean(diff);
      return (
        <div className="flex-row-cascade" style={this.getContainerStyle()}>
          <FixedHeader onHeightChange={this.setHeaderHeight}>

            <div className="bg-light border-bottom border-light container container-wide pvm">
              <div className="columns">
                <div className="column column-one-half">
                  <Button className="button-raw" onClick={this.props.onClearActivePanel}>{this.props.currentGroup.getName()}</Button>
                  <span className="mhs type-weak">→</span>
                  <span>Skill versions</span>
                </div>
                <div className="column column-one-half align-r">
                  {this.renderGithubRepo()}
                </div>
              </div>
            </div>

            <Collapsible revealWhen={this.state.isModifyingGithubRepo}>
              <div className="bg-white border-bottom border-light container container-wide pvm">
                <LinkGithubRepo
                  ref={(el) => this.linkGitHubRepoComponent = el}
                  group={this.props.currentGroup}
                  linked={this.props.linkedGithubRepo}
                  onDoneClick={this.onLinkedGithubRepo}
                  onLinkGithubRepo={this.props.onLinkGithubRepo}
                  csrfToken={this.props.csrfToken}
                />
              </div>
            </Collapsible>

            <div className="bg-lightest border-emphasis-bottom border-pink container container-wide">
              {this.renderVersionSelector(hasChanges)}
            </div>

          </FixedHeader>

          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows bg-white">
              <div className="container container container-wide pvxl">
                {this.renderSelectedVersion(selectedVersion, diff)}
              </div>
            </div>
          </div>

          <FixedFooter onHeightChange={this.setFooterHeight}>

            <Collapsible revealWhen={!this.state.isCommitting}>
              <div className="ptm bg-lightest border-emphasis-top border-pink container container-wide">
                <Button className="mrs mbm button-primary" onClick={this.props.onClearActivePanel}>Done</Button>
                {this.renderSaveButton()}
                {this.renderRevertButton(selectedVersion, hasChanges)}
                {this.renderCommitButton(selectedVersion, hasChanges)}
              </div>
            </Collapsible>
            <Collapsible revealWhen={this.state.isCommitting}>
              <GithubPushPanel
                ref={(el) => this.pushPanel = el}
                group={this.props.currentGroup}
                linked={this.props.linkedGithubRepo}
                onPushBranch={this.onPushBranch}
                onDoneClick={this.toggleCommitting}
                csrfToken={this.props.csrfToken}
              />
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
}

export default VersionBrowser;
