// @flow
import type {Node, Element, ElementType} from 'react';
define(function(require: (string) => *): React.ElementType {
  const React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    BehaviorGroupDiff = require('./behavior_group_diff'),
    Button = require('../../form/button'),
    Editable = require('../../models/editable'),
    Formatter = require('../../lib/formatter'),
    SidebarButton = require('../../form/sidebar_button'),
    Select = require('../../form/select'),
    diffs = require('../../models/diffs'),
    autobind = require('../../lib/autobind');

  type Props = {
    currentGroup: BehaviorGroup,
    currentUserId: string,
    versions: Array<BehaviorGroup>,
    onClearActivePanel: () => void,
    onRestoreClick: (index: number) => void,
    editableIsModified: (editable: Editable) => boolean
  };

  type State = {
    selectedMenuItem: string,
    diffFromSelectedToCurrent: boolean
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
      if (nextProps.versions.length > this.props.versions.length && nextProps.versions.length > 1) {
        this.setState({
          selectedMenuItem: "version1"
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

    nameForVersion(version: BehaviorGroup, index: number): string {
      const author = version.author ? `by ${version.author.formattedName()}` : "";
      if (index === 1) {
        return `Most recently saved (${author})`;
      } else {
        return `${Formatter.formatTimestampShort(version.createdAt)} ${author}`;
      }
    }

    renderVersionOptions(): Node {
      if (this.props.versions.length > 1) {
        return (
          <optgroup label="Local versions">
            {this.props.versions.map((version: BehaviorGroup, index: number): ?Element<SidebarButton> => {
              if (index === 0) {
                return null;
              } else {
                const key = `version${index}`;
                return (
                  <option key={key} value={key}>
                    {this.nameForVersion(version, index)}
                  </option>
                );
              }
            })}
          </optgroup>
        );
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

    getSelectedVersion(index: number): ?BehaviorGroup {
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

    renderSelectedVersion(hasNoChanges: boolean): ElementType {
      const versionIndex = this.getSelectedVersionIndex();
      const selectedVersion = this.getSelectedVersion(versionIndex);
      const diff = this.getDiffForSelectedVersion(selectedVersion);
      return (
        <div>
          {selectedVersion && versionIndex > 0 ? (
            <div>
              <h4>Differences</h4>

              <div className="mbl">
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
          <div>The selected version of the skill is identical to the current version.</div>
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
        </div>
      );
    }
  }

  return VersionBrowser;
});
