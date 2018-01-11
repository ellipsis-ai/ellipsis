// @flow
import type {Node, ElementType} from 'react';
define(function(require: (string) => *): React.ElementType {
  const React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    BehaviorGroupDiff = require('./behavior_group_diff'),
    Button = require('../../form/button'),
    Editable = require('../../models/editable'),
    FixedFooter = require('../../shared_ui/fixed_footer'),
    Formatter = require('../../lib/formatter'),
    Select = require('../../form/select'),
    ToggleGroup = require('../../form/toggle_group'),
    diffs = require('../../models/diffs'),
    autobind = require('../../lib/autobind');

  const versionSources = {
    local: "local",
    github: "github"
  };

  type Props = {
    currentGroup: BehaviorGroup,
    currentUserId: string,
    versions: Array<BehaviorGroup>,
    onClearActivePanel: () => void,
    onRestoreVersionClick: (version: BehaviorGroup, optionalCallback?: () => void) => void,
    editableIsModified: (editable: Editable) => boolean
  };

  type VersionSource = $Keys<typeof versionSources>;

  type State = {
    selectedMenuItem: string,
    diffFromSelectedToCurrent: boolean,
    versionSource: VersionSource,
    footerHeight: number
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
        footerHeight: 0
      };
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

    onClickMenuItem(key: string): void {
      this.setState({
        selectedMenuItem: key
      });
    }

    getLastSavedVersion(): ?BehaviorGroup {
      return this.props.versions[0];
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
          <div className="type-italic">Select a GitHub branch to compare.</div>
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

    renderCurrentVersionPlaceholder(noChanges: boolean): ElementType {
      return (
        <div className="align-button align-button-border mhs">{
          noChanges ? "Current" : "Current (with unsaved changes)"
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

    renderVersionSelector(selectedVersion: ?BehaviorGroup, hasNoChanges: boolean): Node {
      if (this.compareLocalVersions() && selectedVersion) {
        return (
          <div>
            <div className="align-button">From original version:</div>
            {this.state.diffFromSelectedToCurrent ? this.renderSelectableVersion() : this.renderCurrentVersionPlaceholder(hasNoChanges)}
            <div className="align-button">to new version:</div>
            {this.state.diffFromSelectedToCurrent ? this.renderCurrentVersionPlaceholder(hasNoChanges) : this.renderSelectableVersion()}
            <Button onClick={this.invertDiffDirection}>Switch direction</Button>
          </div>
        );
      } else {
        return null;
      }
    }

    render(): ElementType {
      const lastSavedVersion = this.getLastSavedVersion();
      const hasNoChanges = Boolean(lastSavedVersion && lastSavedVersion.isIdenticalTo(this.props.currentGroup));

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

                <h4>Differences</h4>

                {this.renderSelectedVersion(selectedVersion)}
              </div>
            </div>
          </div>

          <FixedFooter ref={(el) => this.footer = el} onHeightChange={this.setFooterHeight}>

            <div className="bg-white-translucent border-emphasis-top border-pink pvl container container-wide">

              <div className="mbl">
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

              <div className="mtl">

                {this.renderVersionSelector(selectedVersion, hasNoChanges)}

              </div>
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
