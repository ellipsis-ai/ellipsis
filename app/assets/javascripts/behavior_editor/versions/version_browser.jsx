// @flow
import type {Node, ElementType} from 'react';
define(function(require: (string) => *): React.ElementType {
  const React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    BehaviorGroupDiff = require('./behavior_group_diff'),
    Button = require('../../form/button'),
    DataRequest = require('../../lib/data_request'),
    DynamicLabelButton = require('../../form/dynamic_label_button'),
    FixedFooter = require('../../shared_ui/fixed_footer'),
    Formatter = require('../../lib/formatter'),
    FormInput = require('../../form/input'),
    GithubErrorNotification = require('../github/github_error_notification'),
    GithubOwnerRepoReadonly = require('../github/github_owner_repo_readonly'),
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
    onChangeGithubLinkClick: () => void,
    onGithubPullClick: () => void,
    onGithubPushClick: () => void,
    onLinkGithubRepo: (string, string, () => void) => void
  };

  type VersionSource = $Keys<typeof versionSources>;

  type State = {
    selectedMenuItem: string,
    diffFromSelectedToCurrent: boolean,
    versionSource: VersionSource,
    footerHeight: number,
    isModifyingGithubRepo: boolean,
    githubBranch: string,
    isFetching: boolean,
    lastFetched: ?Date,
    lastFetchedBranch: ?string,
    githubVersion: ?BehaviorGroup,
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

    constructor(props: Props): void {
      super(props);
      autobind(this);
      this.state = {
        selectedMenuItem: "loading",
        diffFromSelectedToCurrent: true,
        versionSource: versionSources.local,
        footerHeight: 0,
        isModifyingGithubRepo: false,
        githubBranch: "master",
        isFetching: false,
        lastFetched: null,
        lastFetchedBranch: null,
        githubVersion: null,
        error: null
      };
    }

    getBranch(): string {
      return this.state.githubBranch;
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
        }, () => this.updateFromGitHub(owner, repo, branch));
      }
    }

    updateFromGitHub(owner: string, repo: string, branch: string): void {
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

    setVersionSourceToGitHub(): void {
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
        <div className="columns mtxxl">
          <div className="column">
            <img height="32" src="/assets/images/logos/GitHub-Mark-64px.png"/>
          </div>
          <div className="column align-m">
            <span>To push code to or pull code from GitHub, you first need to </span>
            <a href={this.getGithubAuthUrl()}>authenticate your GitHub account.</a>
          </div>
        </div>
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

    shortNameForVersion(version: BehaviorGroup): string {
      return Formatter.formatTimestampShort(version.createdAt);
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

    compareGitHubVersions(): boolean {
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
      } else if (this.compareGitHubVersions()) {
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

    renderSelectedVersion(selectedVersion: ?BehaviorGroup): ElementType {
      if (selectedVersion) {
        const diff = this.getDiffForSelectedVersion(selectedVersion);
        return (
          <div>
            {this.renderDiff(diff)}
          </div>
        );
      } else if (this.compareLocalVersions() && this.props.versions.length === 0) {
        return (
          <div className="pulse">Loading version history…</div>
        );
      } else if (this.compareGitHubVersions()) {
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
          <div className="type-italic">The selected version of the skill is identical to the current version.</div>
        );
      }
    }

    renderSelectableVersion(): ElementType {
      return (
        <Select className="align-b mhs" value={this.state.selectedMenuItem} onChange={this.onClickMenuItem}>
          {this.renderVersionOptions()}
        </Select>
      );
    }

    renderCurrentVersionPlaceholder(): ElementType {
      return (
        <div className="align-button align-button-border mhs">{
          this.props.currentGroupIsModified ? "Current" : "Current (with unsaved changes)"
        }</div>
      );
    }

    renderRevertButton(): ElementType {
      const selectedVersion = this.getSelectedVersion();
      if (selectedVersion && !selectedVersion.isIdenticalTo(this.props.currentGroup)) {
        return (
          <Button className="mrs mbm" onClick={this.revertToSelected}>Revert to {this.shortNameForVersion(selectedVersion)}</Button>
        );
      } else {
        return (
          <Button className="mrs mbm" disabled={true}>Revert…</Button>
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
      if (!this.props.isLinkedToGithub) {
        return this.renderGithubAuth();
      } else if (!this.props.linkedGithubRepo || this.state.isModifyingGithubRepo) {
        return (
          <LinkGithubRepo
            group={this.props.currentGroup}
            linked={this.props.linkedGithubRepo}
            onDoneClick={this.onLinkedGithubRepo}
            onLinkGithubRepo={this.props.onLinkGithubRepo}
            csrfToken={this.props.csrfToken}
          />
        );
      } else {
        return (
          <div>
            <div className="mts mbxl">
              <span className="mrm">
                <span className="mrs">Repository:</span>
                <GithubOwnerRepoReadonly linked={this.props.linkedGithubRepo} />
              </span>
              <Button className="button-s button-shrink" onClick={this.onChangeGithubLinkClick}>Change repo…</Button>
            </div>
            <div className="mbl">
              <span className="align-button mrs">{this.state.diffFromSelectedToCurrent ? "From branch:" : "From:"}</span>
              {this.state.diffFromSelectedToCurrent ? this.renderFromBranch() : this.renderCurrentVersionPlaceholder()}
              <span className="align-button mrs">{this.state.diffFromSelectedToCurrent ? "to:" : "to branch:"}</span>
              {this.state.diffFromSelectedToCurrent ? this.renderCurrentVersionPlaceholder() : this.renderFromBranch()}
              <Button onClick={this.invertDiffDirection} className="mrm" disabled={this.state.isFetching || !this.getSelectedVersion()}>Switch direction</Button>
              <div className="align-button">
                {this.renderGithubStatus()}
              </div>
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
            <div className="align-button">From original version:</div>
            {this.state.diffFromSelectedToCurrent ? this.renderSelectableVersion() : this.renderCurrentVersionPlaceholder()}
            <div className="align-button">to new version:</div>
            {this.state.diffFromSelectedToCurrent ? this.renderCurrentVersionPlaceholder() : this.renderSelectableVersion()}
            <Button onClick={this.invertDiffDirection}>Switch direction</Button>
          </div>
        );
      } else if (this.compareGitHubVersions()) {
        return this.renderGithubVersionSelector();
      } else {
        return null;
      }
    }

    renderDiffTitle(): Node {
      if (this.compareGitHubVersions() && this.state.lastFetched && this.state.lastFetchedBranch) {
        return (
          <h4>
            <span>Differences from branch </span>
            <code>{this.state.lastFetchedBranch}</code>
            <span> ({Formatter.formatTimestampShort(this.state.lastFetched)}) on GitHub</span>
          </h4>
        );
      } else if (this.compareLocalVersions() && this.getSelectedVersionIndex() > 0) {
        return (
          <h4>Differences from version dated {this.shortNameForVersion(this.getSelectedVersion())}</h4>
        );
      } else {
        return (
          <h4>Differences</h4>
        );
      }
    }

    render(): ElementType {
      const selectedVersion = this.getSelectedVersion();
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

                {this.renderDiffTitle()}

                {this.renderSelectedVersion(selectedVersion)}
              </div>
            </div>
          </div>

          <FixedFooter ref={(el) => this.footer = el} onHeightChange={this.setFooterHeight}>

            <div className="bg-lightest border-emphasis-top border-pink pvl container container-wide">

              <div>
                <span className="align-button mrs">
                  Compare with:
                </span>
                <ToggleGroup>
                  <ToggleGroup.Item
                    onClick={this.setVersionSourceToLocal}
                    activeWhen={this.compareLocalVersions()}
                    label={"Versions saved in Ellipsis"}
                  />
                  <ToggleGroup.Item
                    onClick={this.setVersionSourceToGitHub}
                    activeWhen={this.compareGitHubVersions()}
                    label={"Versions on GitHub"}
                  />
                </ToggleGroup>
              </div>

            </div>

            <div className="bg-white-translucent border-top pvl border-light container container-wide">

              {this.renderVersionSelector(selectedVersion)}

            </div>

            <div className="ptm bg-lightest border-top border-light container container-wide">
              <Button className="mrs mbm button-primary" onClick={this.props.onClearActivePanel}>Done</Button>
              {this.renderRevertButton()}
            </div>

          </FixedFooter>
        </div>
      );
    }
  }

  return VersionBrowser;
});
