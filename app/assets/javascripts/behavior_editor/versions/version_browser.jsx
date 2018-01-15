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
    FixedFooter = require('../../shared_ui/fixed_footer'),
    Formatter = require('../../lib/formatter'),
    FormInput = require('../../form/input'),
    GithubErrorNotification = require('../github/github_error_notification'),
    GithubOwnerRepoReadonly = require('../github/github_owner_repo_readonly'),
    GithubPushPanel = require('../github/github_push_panel'),
    LinkGithubRepo = require('./link_github_repo'),
    LinkedGithubRepo = require('../../models/linked_github_repo'),
    Select = require('../../form/select'),
    ToggleGroup = require('../../form/toggle_group'),
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
    versions: Array<BehaviorGroup>,
    onClearActivePanel: () => void,
    onRestoreVersionClick: (version: BehaviorGroup, optionalCallback?: () => void) => void,
    isLinkedToGithub: boolean,
    linkedGithubRepo?: LinkedGithubRepo,
    onLinkGithubRepo: (string, string, () => void) => void,
    onSaveChanges: () => void
  };

  type VersionSource = $Keys<typeof versionSources>;

  type State = {
    selectedMenuItem: string,
    diffFromSelectedToCurrent: boolean,
    versionSource: VersionSource,
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
    label: string,
    version: BehaviorGroup
  }

  type VersionGroup = {
    label: string,
    versions: Array<GroupedVersion>
  }

  class VersionBrowser extends React.Component<Props, State> {
    props: Props;
    state: State;
    footer: ?HTMLDivElement;
    scrollContainer: ?HTMLDivElement;
    pushPanel: ?GithubPushPanel;

    constructor(props: Props): void {
      super(props);
      autobind(this);
      this.state = {
        selectedMenuItem: "loading",
        diffFromSelectedToCurrent: true,
        versionSource: versionSources.local,
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

    setVersionSourceToLocal(): void {
      this.setState({
        versionSource: versionSources.local
      });
    }

    setVersionSourceToGithub(): void {
      this.setState({
        versionSource: versionSources.github
      });
    }

    componentWillReceiveProps(nextProps: Props): void {
      if (nextProps.versions.length !== this.props.versions.length) {
        this.setState({
          selectedMenuItem: nextProps.versions.length > 0 ? "version0" : "loading"
        });
      }
    }

    getGithubAuthUrl(): string {
      const redirect = jsRoutes.controllers.BehaviorEditorController.edit(this.props.currentGroup.id).url;
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

    getGroupedVersions(versions: Array<BehaviorGroup>): Array<VersionGroup> {
      const groups: Array<VersionGroup> = [];
      versions.forEach((version, versionIndex) => {
        if (versionIndex === 0) {
          groups.push({
            label: `Most recent saved version (${this.authorForVersion(version)})`,
            versions: [{
              label: Formatter.formatTimestampShort(version.createdAt),
              key: "version0",
              version: version
            }]
          });
        } else if (versionIndex > 0) {
          const author = version.author;
          const day = Formatter.formatTimestampDate(version.createdAt);
          const prevVersion = versions[versionIndex - 1];
          const prevAuthor = prevVersion && prevVersion.author;
          const prevDay = prevVersion && Formatter.formatTimestampDate(prevVersion.createdAt);
          const groupedVersion = {
            label: Formatter.formatTimestampShort(version.createdAt),
            key: `version${versionIndex}`,
            version: version
          };
          if (versionIndex > 1 && author.isSameUser(prevAuthor) && day === prevDay) {
            const lastGroup = groups[groups.length - 1];
            lastGroup.versions.push(groupedVersion);
          } else {
            groups.push({
              label: `Saved on ${day} ${this.authorForVersion(version)}`,
              versions: [groupedVersion]
            });
          }
        }
      });
      return groups;
    }

    renderVersionOptions(): Node {
      if (this.props.versions.length > 0) {
        return this.getGroupedVersions(this.props.versions).map((versionGroup, groupIndex) => (
          <optgroup label={versionGroup.label} key={`versionGroup${groupIndex}`}>
            {versionGroup.versions.map((groupedVersion) => (
              <option key={groupedVersion.key} value={groupedVersion.key}>{groupedVersion.label}</option>
            ))}
          </optgroup>
        ));
      } else {
        return (
          <option className="pulse type-disabled" value="loading">Loading versions…</option>
        );
      }
    }

    compareLocalVersions(): boolean {
      return this.getVersionSource() === versionSources.local;
    }

    compareGithubVersions(): boolean {
      return this.getVersionSource() === versionSources.github;
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

    getVersionSource(): VersionSource {
      return this.state.versionSource;
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
          <div className="type-italic">Fetch a GitHub branch to compare.</div>
        );
      } else {
        return (
          <div className="type-italic">Select another version to compare.</div>
        );
      }
    }

    renderDiff(diff: ?diffs.ModifiedDiff<BehaviorGroup>): Node {
      if (diff) {
        return (
          <BehaviorGroupDiff diff={diff} />
        );
      } else {
        return (
          <div className="type-italic">These versions are identical.</div>
        );
      }
    }

    renderSelectableVersion(): ElementType {
      return (
        <Select className="align-b mrs" value={this.state.selectedMenuItem} onChange={this.onClickMenuItem}>
          {this.renderVersionOptions()}
        </Select>
      );
    }

    renderCurrentVersionPlaceholder(): ElementType {
      return (
        <div className="align-button align-button-border mrs">{
          this.props.currentGroupIsModified ? "Current (with unsaved changes)" : "Current"
        }</div>
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
            <span>Pull changes from <span className="type-monospace">{this.state.lastFetchedBranch}</span>…</span>
          );
        } else {
          return "Pull…";
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

    renderCommitButton(hasChanges: boolean): Node {
      if (this.props.linkedGithubRepo && this.compareGithubVersions()) {
        return (
          <Button onClick={this.toggleCommitting} disabled={!hasChanges || this.props.currentGroupIsModified} className="mrs mbm">Commit changes to GitHub…</Button>
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
      });
    }

    renderGithubVersionSelector(): Node {
      if (this.props.isLinkedToGithub && this.props.linkedGithubRepo) {
        return (
          <div>
            <span className="align-button mrs">{this.state.diffFromSelectedToCurrent ? "From GitHub branch:" : "From:"}</span>
            {this.state.diffFromSelectedToCurrent ? this.renderFromBranch() : this.renderCurrentVersionPlaceholder()}
            <span className="align-button mrs">{this.state.diffFromSelectedToCurrent ? "to:" : "to GitHub branch:"}</span>
            {this.state.diffFromSelectedToCurrent ? this.renderCurrentVersionPlaceholder() : this.renderFromBranch()}
            <Button onClick={this.invertDiffDirection} className="mrm" disabled={this.state.isFetching || !this.getSelectedVersion()}>Switch direction</Button>
            <div className="align-button">
              {this.renderGithubStatus()}
            </div>
          </div>
        );
      }
    }

    renderFromBranch(): ElementType {
      return (
        <div className="display-inline-block">
          <FormInput
            ref={(el) => this.branchInput = el}
            className="form-input-borderless type-monospace type-s width-15 mrs"
            placeholder="e.g. master"
            onChange={this.onBranchChange}
            value={this.getBranch()}
          />
          <DynamicLabelButton
            className="button-shrink mrm"
            onClick={this.onUpdateFromGithub}
            disabledWhen={this.state.isFetching || !this.getBranch()}
            labels={[{
              text: "Fetch",
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
          <GithubErrorNotification error={this.state.error}/>
        );
      }
    }

    renderVersionSelector(selectedVersion: ?BehaviorGroup): Node {
      if (this.compareLocalVersions() && selectedVersion) {
        return (
          <div>
            <div className="align-button mrs">From original version:</div>
            {this.state.diffFromSelectedToCurrent ? this.renderSelectableVersion() : this.renderCurrentVersionPlaceholder()}
            <div className="align-button mrs">to new version:</div>
            {this.state.diffFromSelectedToCurrent ? this.renderCurrentVersionPlaceholder() : this.renderSelectableVersion()}
            <Button onClick={this.invertDiffDirection}>Switch direction</Button>
          </div>
        );
      } else if (this.compareGithubVersions()) {
        return this.renderGithubVersionSelector();
      } else {
        return (
          <div>
            <div className="align-button pulse type-italic type-weak">Loading…</div>
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

    renderVersionTitle(version: ?BehaviorGroup): Node {
      if (this.compareGithubVersions() && this.state.lastFetched && this.state.lastFetchedBranch) {
        return this.renderBranchTitle(this.state.lastFetchedBranch, this.state.lastFetched);
      } else if (this.compareLocalVersions() && version) {
        return this.renderLocalVersionTitle(version.createdAt);
      } else {
        return null;
      }
    }

    renderDiffTitle(version: ?BehaviorGroup): Node {
      const versionTitle = this.renderVersionTitle(version);
      if (versionTitle) {
        return (
          <h4>
            {this.state.diffFromSelectedToCurrent ? (
              <span>Changes from {versionTitle} to current</span>
            ) : (
              <span>Changes from current to {versionTitle}</span>
            )}
          </h4>
        );
      } else {
        return (
          <h4>Changes</h4>
        );
      }
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

    render(): ElementType {
      const selectedVersion = this.getSelectedVersion();
      const diff = this.getDiffForSelectedVersion(selectedVersion);
      const hasChanges = Boolean(diff);
      return (
        <div ref={(el) => this.scrollContainer = el} className="flex-row-cascade" style={{ paddingBottom: `${this.getFooterHeight()}px` }}>
          <div className="bg-lightest">

            <div className="container container-wide pvm">
              <Button className="button-raw" onClick={this.props.onClearActivePanel}>{this.props.currentGroup.getName()}</Button>
              <span className="mhs type-weak">→</span>
              <span>Compare skill versions</span>
            </div>

          </div>

          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows bg-white">
              <div className="container container container-wide ptm pbxl">

                {this.renderDiffTitle(selectedVersion)}

                {this.renderSelectedVersion(selectedVersion, diff)}
              </div>
            </div>
          </div>

          <FixedFooter ref={(el) => this.footer = el} onHeightChange={this.setFooterHeight}>

            <Collapsible revealWhen={!this.state.isCommitting}>
              <div className="bg-lightest border-emphasis-top border-pink pvl container container-wide">

                <div className="columns">
                  <div className="column column-one-half">
                    {this.props.linkedGithubRepo && this.props.isLinkedToGithub ? (
                      <div>
                        <span className="type-label align-m display-inline-block mrs">
                          Compare with:
                        </span>
                        <ToggleGroup className="form-toggle-group-s mrm">
                          <ToggleGroup.Item
                            onClick={this.setVersionSourceToLocal}
                            activeWhen={this.compareLocalVersions()}
                            label={"Versions saved in Ellipsis"}
                          />
                          <ToggleGroup.Item
                            onClick={this.setVersionSourceToGithub}
                            activeWhen={this.compareGithubVersions()}
                            label={"Versions on GitHub"}
                          />
                        </ToggleGroup>
                      </div>
                    ) : (
                      <span className="type-label">Compare with saved versions:</span>
                    )}
                  </div>
                  <div className="column column-one-half align-r">
                    {this.renderGithubRepo()}
                  </div>
                </div>

                <Collapsible revealWhen={this.state.isModifyingGithubRepo}>
                  <div className="ptxl">
                    <LinkGithubRepo
                      group={this.props.currentGroup}
                      linked={this.props.linkedGithubRepo}
                      onDoneClick={this.onLinkedGithubRepo}
                      onLinkGithubRepo={this.props.onLinkGithubRepo}
                      csrfToken={this.props.csrfToken}
                    />
                  </div>
                </Collapsible>

              </div>

              <div className="bg-white-translucent border-top pvl border-light container container-wide">

                {this.renderVersionSelector(selectedVersion)}

              </div>

              <div className="ptm bg-lightest border-top border-light container container-wide">
                <Button className="mrs mbm button-primary" onClick={this.props.onClearActivePanel}>Done</Button>
                {this.renderSaveButton()}
                {this.renderRevertButton(selectedVersion, hasChanges)}
                {this.renderCommitButton(hasChanges)}
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
