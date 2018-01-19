// @flow
import type {Node, ElementType} from 'react';
import type {Timestamp} from '../../lib/formatter';
define(function(require: (string) => *): React.ElementType {
  const React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    BehaviorGroupDiff = require('./behavior_group_diff'),
    Button = require('../../form/button'),
    Collapsible = require('../../shared_ui/collapsible'),
    DataRequest = require('../../lib/data_request'),
    DynamicLabelButton = require('../../form/dynamic_label_button'),
    FixedHeader = require('../../shared_ui/fixed_header'),
    FixedFooter = require('../../shared_ui/fixed_footer'),
    Formatter = require('../../lib/formatter'),
    FormInput = require('../../form/input'),
    GithubErrorNotification = require('../github/github_error_notification'),
    GithubOwnerRepoReadonly = require('../github/github_owner_repo_readonly'),
    GithubPushPanel = require('../github/github_push_panel'),
    LinkGithubRepo = require('./link_github_repo'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    Select = require('../../form/select'),
    diffs = require('../../models/diffs'),
    autobind = require('../../lib/autobind');

  const versionSources = {
    local: "local",
    github: "github"
  };

  type Props = {
    csrfToken: string,
    currentGroup: BehaviorGroup,
    currentGroupIsModified: boolean,
    currentUserId: string,
    currentSelectedId?: string,
    versions: Array<BehaviorGroup>,
    onClearActivePanel: () => void,
    onRestoreVersionClick: (version: BehaviorGroup, optionalCallback?: () => void) => void,
    isLinkedToGithub: boolean,
    linkedGithubRepo?: LinkedGithubRepo,
    onLinkGithubRepo: (string, string, () => void) => void,
    onSaveChanges: () => void
  };

  type State = {
    selectedMenuItem: string,
    diffFromSelectedToCurrent: boolean,
    headerHeight: number,
    footerHeight: number,
    isModifyingGithubRepo: boolean,
    branch: string,
    isFetching: boolean,
    lastFetched: ?Date,
    lastFetchedBranch: ?string,
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

    constructor(props: Props): void {
      super(props);
      autobind(this);
      this.state = {
        selectedMenuItem: "loading",
        diffFromSelectedToCurrent: true,
        headerHeight: 0,
        footerHeight: 0,
        isModifyingGithubRepo: false,
        branch: "master",
        isFetching: false,
        lastFetched: null,
        lastFetchedBranch: null,
        githubVersion: null,
        isCommitting: false,
        error: null
      };
    }

    onPushBranch(): void {
      this.setState({
        githubVersion: this.props.currentGroup,
        lastFetched: new Date()
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

    getBranch(): string {
      return this.state.branch;
    }

    onBranchChange(branch: string): void {
      this.setState({
        branch: Formatter.formatGitBranchIdentifier(branch)
      });
    }

    onUpdateFromGithub(): void {
      if (this.props.linkedGithubRepo) {
        const linked = this.props.linkedGithubRepo;
        const owner = linked.getOwner();
        const repo = linked.getRepo();
        const branch = this.getBranch();
        this.setState({
          isFetching: true,
          error: null
        }, () => this.updateFromGithub(owner, repo, branch));
      }
    }

    updateFromGithub(owner: string, repo: string, branch: string): void {
      DataRequest.jsonPost(
        jsRoutes.controllers.BehaviorEditorController.updateFromGithub().url, {
          behaviorGroupId: this.props.currentGroup.id,
          owner: owner,
          repo: repo,
          branch: branch
        },
        this.props.csrfToken
      ).then((json) => {
        if (json.errors) {
          this.onError(branch, json.errors);
        } else {
          this.setState({
            isFetching: false,
            lastFetched: new Date(),
            lastFetchedBranch: branch,
            githubVersion: BehaviorGroup.fromJson(json.data)
          });
        }
      }).catch(() => {
        this.onError(branch);
      });
    }

    onError(branch: string, error?: string): void {
      this.setState({
        isFetching: false,
        error: error ? `Error: ${error}` : `An error occurred while pulling “${branch}” from GitHub`
      });
    }

    getDefaultSelectedItem(props: Props): string {
      return props.versions.length > 0 ? "version0" : "loading";
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

    renderGithubAuth(): Node {
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
      });
    }

    authorForVersion(version: BehaviorGroup): string {
      const isCurrentUser = version.author && version.author.id === this.props.currentUserId;
      return version.author ? `by ${isCurrentUser ? "you" : version.author.formattedName()}` : "";
    }

    shortNameForVersion(version: BehaviorGroup): Node {
      if (this.compareGithubVersions() && this.state.lastFetched && this.state.lastFetchedBranch) {
        return this.renderBranchTitle(this.state.lastFetchedBranch, this.state.lastFetched);
      } else {
        return this.renderLocalVersionTitle(version.createdAt);
      }
    }

    getLabelForLastSavedVersion(): string {
      return this.props.currentGroupIsModified ? "Most recent saved version" : "Select a version…";
    }

    getGroupedVersions(versions: Array<BehaviorGroup>): Array<VersionGroup> {
      const groups: Array<VersionGroup> = [];
      groups.push({
        versions: [{
          label: this.getLabelForLastSavedVersion(),
          key: "version0"
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
      versions.slice(1).forEach((version, versionIndex) => {
        const author = version.author;
        const day = Formatter.formatTimestampDate(version.createdAt);
        const prevVersion = versions[versionIndex - 1];
        const prevAuthor = prevVersion && prevVersion.author;
        const prevDay = prevVersion && Formatter.formatTimestampDate(prevVersion.createdAt);
        const groupedVersion = {
          label: Formatter.formatTimestampShort(version.createdAt),
          key: `version${versionIndex + 1}`
        };
        if (versionIndex > 0 && author.isSameUser(prevAuthor) && day === prevDay) {
          const lastGroup = groups[groups.length - 1];
          lastGroup.versions.push(groupedVersion);
        } else {
          groups.push({
            label: `Saved on ${day} ${this.authorForVersion(version)}`,
            versions: [groupedVersion]
          });
        }
      });
      return groups;
    }

    renderVersionGroup(versions: Array<GroupedVersion>): Node {
      return versions.map((groupedVersion) => (
        <option key={groupedVersion.key} value={groupedVersion.key}>{groupedVersion.label}</option>
      ));
    }

    renderVersionOptions(): Node {
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

    compareLocalVersions(): boolean {
      return this.state.selectedMenuItem !== versionSources.github;
    }

    compareGithubVersions(): boolean {
      return this.state.selectedMenuItem === versionSources.github;
    }

    getSelectedVersionIndex(): number {
      if (this.compareLocalVersions() && this.state.selectedMenuItem) {
        const match = this.state.selectedMenuItem.match(/version(\d+)/);
        const index = match ? parseInt(match[1], 10) : null;
        return index || 0;
      } else {
        return 0;
      }
    }

    getSelectedVersion(): ?BehaviorGroup {
      if (this.compareLocalVersions()) {
        return this.getVersionIndex(this.getSelectedVersionIndex());
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

    getDiffForSelectedVersion(selectedVersion: ?BehaviorGroup): ?diffs.ModifiedDiff<BehaviorGroup> {
      // original.maybeDiffFor(modified) shows changes from original to modified
      if (selectedVersion) {
        return this.state.diffFromSelectedToCurrent ?
          diffs.maybeDiffFor(selectedVersion, this.props.currentGroup) :
          diffs.maybeDiffFor(this.props.currentGroup, selectedVersion);
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
      if (selected) {
        this.props.onRestoreVersionClick(selected);
        this.props.onClearActivePanel();
      }
    }

    renderSelectedVersion(selectedVersion: ?BehaviorGroup, diff: ?diffs.ModifiedDiff<BehaviorGroup>): ElementType {
      if (selectedVersion) {
        return (
          <div>
            {this.renderDiff(diff)}
          </div>
        );
      } else if (this.compareLocalVersions() && this.props.versions.length === 0) {
        return (
          <div className="pulse">Loading version history…</div>
        );
      } else if (this.compareGithubVersions()) {
        return (
          <div className="type-italic">Select a GitHub branch to compare.</div>
        );
      } else {
        return (
          <div className="type-italic">Select another version to compare.</div>
        );
      }
    }

    summarizeNoDiff(): string {
      if (this.getSelectedVersionIndex() === 0 && !this.props.currentGroupIsModified) {
        return "Select another version to compare to the current saved version.";
      } else {
        return "These versions are identical.";
      }
    }

    renderDiff(diff: ?diffs.ModifiedDiff<BehaviorGroup>): Node {
      if (diff) {
        return (
          <BehaviorGroupDiff diff={diff} />
        );
      } else {
        return (
          <div className="type-italic">{this.summarizeNoDiff()}</div>
        );
      }
    }

    renderSelectableVersion(caption: Node): ElementType {
      return (
        <div>
          {caption}
          <Select className="align-b form-select-s mrs mbs" value={this.state.selectedMenuItem} onChange={this.onClickMenuItem}>
            {this.renderVersionOptions()}
          </Select>
          {this.renderSelectedVersionNote()}
          {this.compareGithubVersions() ? this.renderGithubBranchInput() : null}
          {this.compareGithubVersions() ? this.renderGithubStatus() : null}
        </div>
      );
    }

    latestVersionIsSelected(): boolean {
      return this.state.selectedMenuItem === "version0";
    }

    renderNoteForVersion(version: BehaviorGroup): Node {
      return (
        <span className="align-button align-button-s mrs mbs type-weak">({Formatter.formatTimestampShort(version.createdAt)})</span>
      );
    }

    renderSelectedVersionNote(): Node {
      const version = this.props.versions[0];
      if (this.latestVersionIsSelected() && version && this.props.currentGroupIsModified) {
        return this.renderNoteForVersion(version);
      }
    }

    renderCurrentVersionNote(): Node {
      if (!this.props.currentGroupIsModified) {
        return this.renderNoteForVersion(this.props.currentGroup);
      }
    }

    renderCurrentVersionPlaceholder(caption: Node): ElementType {
      return (
        <div>
          {caption}
          <div className="align-button align-button-s align-button-border mrs mbs">
            {this.props.currentGroupIsModified ? "Current version (unsaved)" : "Current saved version"}
          </div>
          {this.renderCurrentVersionNote()}
        </div>
      );
    }

    renderSaveButton(): Node {
      if (this.props.currentGroupIsModified) {
        return (
          <Button className="mrs mbm" onClick={this.props.onSaveChanges}>
            Save changes
          </Button>
        );
      }
    }

    renderRevertButton(selectedVersion: ?BehaviorGroup, hasChanges: boolean): ElementType {
      return (
        <Button className="mrs mbm" onClick={this.revertToSelected} disabled={!hasChanges}>
          {this.renderRevertButtonTitle(selectedVersion, hasChanges)}
        </Button>
      );
    }

    renderRevertButtonTitle(selectedVersion: ?BehaviorGroup, hasChanges: boolean): Node {
      if (this.compareGithubVersions()) {
        if (selectedVersion && hasChanges && this.state.lastFetchedBranch) {
          return (
            <span>Checkout <span className="type-monospace">{this.state.lastFetchedBranch}</span> as current version…</span>
          );
        } else {
          return "Checkout…";
        }
      } else {
        if (selectedVersion && hasChanges) {
          return (
            <span>Revert to {this.shortNameForVersion(selectedVersion)}…</span>
          );
        } else {
          return "Revert…";
        }
      }
    }

    renderCommitButton(): Node {
      if (this.props.linkedGithubRepo && this.compareGithubVersions()) {
        return (
          <Button onClick={this.toggleCommitting} disabled={this.props.currentGroupIsModified} className="mrs mbm">Commit current version to GitHub…</Button>
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

    renderGithubBranchInput(): ElementType {
      return (
        <div className="display-inline-block mbs">
          <FormInput
            ref={(el) => this.branchInput = el}
            className="form-input-borderless form-input-s type-monospace width-15 mrs"
            placeholder="Branch (e.g. master)"
            onChange={this.onBranchChange}
            value={this.getBranch()}
          />
          <DynamicLabelButton
            className="button-shrink button-s"
            onClick={this.onUpdateFromGithub}
            disabledWhen={this.state.isFetching || !this.getBranch()}
            labels={[{
              text: this.state.lastFetchedBranch === this.getBranch() ? "Refresh branch" : "Select branch",
              displayWhen: !this.state.isFetching
            }, {
              text: "Fetching…",
              displayWhen: this.state.isFetching
            }]}
          />
        </div>
      );
    }

    renderGithubStatus(): Node {
      if (this.state.error) {
        return (
          <div className="align-button align-button-s mbs">
            <GithubErrorNotification error={this.state.error}/>
          </div>
        );
      }
    }

    renderLeftCaption(): Node {
      return (
        <div className="align-button align-button-s mrs mbs">Compare</div>
      );
    }

    renderRightCaption(): Node {
      return (
        <div className="align-button align-button-s mrs mbs">To</div>
      );
    }

    renderVersionSelector(hasChanges: boolean): Node {
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

    renderBranchTitle(branchName: string, timestamp: Timestamp): Node {
      return (
        <span>
          <span className="type-monospace">{branchName}</span>
          <span> branch ({Formatter.formatTimestampShort(timestamp)})</span>
        </span>
      );
    }

    renderLocalVersionTitle(timestamp: Timestamp): Node {
      return this.getSelectedVersionIndex() === 0 ? (
        <span>last saved version</span>
      ) : (
        <span>version dated {Formatter.formatTimestampShort(timestamp)}</span>
      );
    }

    renderGithubRepo(): Node {
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

    renderChangeRepoButton(): Node {
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

    render(): ElementType {
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
                {this.renderCommitButton()}
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
                branch={this.state.lastFetchedBranch}
              />
            </Collapsible>
          </FixedFooter>
        </div>
      );
    }
  }

  return VersionBrowser;
});
