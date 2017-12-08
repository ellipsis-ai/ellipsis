// @flow
import type {Node, ElementType} from 'react';
define(function(require: (string) => *): React.ElementType {
  const React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    BehaviorGroupDiff = require('./behavior_group_diff'),
    Button = require('../../form/button'),
    Editable = require('../../models/editable'),
    Formatter = require('../../lib/formatter'),
    Select = require('../../form/select'),
    diffs = require('../../models/diffs'),
    autobind = require('../../lib/autobind');

  type Props = {
    currentGroup: BehaviorGroup,
    currentUserId: string,
    versions: Array<BehaviorGroup>,
    onClearActivePanel: () => void,
    onRestoreClick: (index: number, optionalCallback?: () => void) => void,
    editableIsModified: (editable: Editable) => boolean
  };

  type State = {
    selectedMenuItem: string,
    diffFromSelectedToCurrent: boolean
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

  class VersionBrowser extends React.Component<Props> {
    props: Props;
    state: State;

    constructor(props: Props): void {
      super(props);
      autobind(this);
      this.state = {
        selectedMenuItem: "loading",
        diffFromSelectedToCurrent: true
      };
    }

    componentWillReceiveProps(nextProps: Props): void {
      if (nextProps.versions.length !== this.props.versions.length) {
        this.setState({
          selectedMenuItem: nextProps.versions.length > 1 ? "version1" : "loading"
        });
      }
    }

    onClickMenuItem(key: string): void {
      this.setState({
        selectedMenuItem: key
      });
    }

    getLastSavedVersion(): ?BehaviorGroup {
      return this.props.versions[1];
    }

    authorForVersion(version: BehaviorGroup): string {
      const isCurrentUser = version.author && version.author.id === this.props.currentUserId;
      return version.author ? `by ${isCurrentUser ? "you" : version.author.formattedName()}` : "";
    }

    shortNameForVersion(version: BehaviorGroup, index: number): string {
      if (index === 1) {
        return `last saved version`;
      } else {
        return `${Formatter.formatTimestampShort(version.createdAt)}`;
      }
    }

    getGroupedVersions(versions: Array<BehaviorGroup>): Array<VersionGroup> {
      const groups: Array<VersionGroup> = [];
      versions.forEach((version, versionIndex) => {
        if (versionIndex === 1) {
          groups.push({
            label: `Most recent saved version (${this.authorForVersion(version)})`,
            versions: [{
              label: Formatter.formatTimestampShort(version.createdAt),
              key: "version1",
              version: version
            }]
          });
        } else if (versionIndex > 1) {
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
          if (versionIndex > 2 && author.isSameUser(prevAuthor) && day === prevDay) {
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
      if (this.props.versions.length > 1) {
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

    getSelectedVersionIndex(): number {
      if (this.state.selectedMenuItem) {
        const match = this.state.selectedMenuItem.match(/version(\d+)/);
        const index = match ? parseInt(match[1], 10) : null;
        return index || 0;
      } else {
        return 0;
      }
    }

    getVersionIndex(index: number): ?BehaviorGroup {
      return this.props.versions[index] || this.props.currentGroup;
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
          selectedVersion.maybeDiffFor(this.props.currentGroup) :
          this.props.currentGroup.maybeDiffFor(selectedVersion);
      } else {
        return null;
      }
    }

    revertToSelected(): void {
      this.props.onRestoreClick(this.getSelectedVersionIndex());
      this.props.onClearActivePanel();
    }

    renderSelectedVersion(hasNoChanges: boolean): ElementType {
      const versionIndex = this.getSelectedVersionIndex();
      const selectedVersion = this.getVersionIndex(versionIndex);
      const diff = this.getDiffForSelectedVersion(selectedVersion);
      return (
        <div>
          {selectedVersion && versionIndex > 0 ? (
            <div>
              <h4>Differences</h4>

              <div className="mbxl">
                <div className="align-button">From original version:</div>
                {this.state.diffFromSelectedToCurrent ? this.renderSelectableVersion() : this.renderCurrentVersionPlaceholder(hasNoChanges)}
                <div className="align-button">to new version:</div>
                {this.state.diffFromSelectedToCurrent ? this.renderCurrentVersionPlaceholder(hasNoChanges) : this.renderSelectableVersion()}
                <Button onClick={this.invertDiffDirection}>Switch direction</Button>
              </div>

              {this.renderDiff(diff)}
            </div>
          ) : (
            <span className="pulse">Loading…</span>
          )}
        </div>
      );
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
      const index = this.getSelectedVersionIndex();
      const selectedVersion = this.getVersionIndex(index);
      if (selectedVersion && !selectedVersion.isIdenticalTo(this.props.currentGroup)) {
        return (
          <Button className="mrs mbm" onClick={this.revertToSelected}>Revert to {this.shortNameForVersion(selectedVersion, index)}</Button>
        );
      } else {
        return (
          <Button className="mrs mbm" disabled={true}>Revert…</Button>
        );
      }
    }

    render(): ElementType {
      const lastSavedVersion = this.getLastSavedVersion();
      const hasNoChanges = Boolean(lastSavedVersion && lastSavedVersion.isIdenticalTo(this.props.currentGroup));
      return (
        <div className="flex-row-cascade pbxxxxl">
          <div className="bg-white container container-wide pvm border-bottom border-light">

            <Button className="button-raw" onClick={this.props.onClearActivePanel}>{this.props.currentGroup.getName()}</Button>
            <span className="mhs type-weak">→</span>
            <span>Skill versions</span>

          </div>
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows bg-white">
              <div className="container container container-wide pvm">
                {this.renderSelectedVersion(hasNoChanges)}
              </div>
            </div>
          </div>

          <div className="position-fixed-bottom container container-wide ptm border-top bg-white-translucent">
            <Button className="mrs mbm button-primary" onClick={this.props.onClearActivePanel}>Done</Button>
            {this.renderRevertButton()}
          </div>
        </div>
      );
    }
  }

  return VersionBrowser;
});
