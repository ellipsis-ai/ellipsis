// @flow
import type {Node, Element, ElementType} from 'react';
define(function(require: (string) => *): React.ElementType {
  const React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    BehaviorGroupDiff = require('./behavior_group_diff'),
    BehaviorGroupSaveInfo = require('../behavior_group_save_info'),
    Button = require('../../form/button'),
    Editable = require('../../models/editable'),
    Formatter = require('../../lib/formatter'),
    SidebarButton = require('../../form/sidebar_button'),
    ToggleGroup = require('../../form/toggle_group'),
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
    selectedMenuItem: ?string,
    diffFromSelectedToCurrent: boolean
  }

  class VersionBrowser extends React.Component<Props> {
    props: Props;
    state: State;

    constructor(props: Props): void {
      super(props);
      autobind(this);
      this.state = {
        selectedMenuItem: null,
        diffFromSelectedToCurrent: true
      };
    }

    onClickMenuItem(key: ?string): void {
      this.setState({
        selectedMenuItem: key
      });
    }

    getLastSavedVersion(): ?BehaviorGroup {
      return this.props.versions[1];
    }

    nameForVersion(version: BehaviorGroup, index: number, previousItem: ?BehaviorGroup, shouldFilterLastSavedVersion: boolean): string {
      if (index === 0) {
        if (shouldFilterLastSavedVersion) {
          return "Current version (saved)";
        } else {
          return "Current version (unsaved changes)";
        }
      } else if (index === 1) {
        return "Last saved version";
      } else if (index === 2) {
        return Formatter.formatTimestampShort(version.createdAt);
      } else {
        const previousDay = previousItem ? Formatter.formatTimestampDate(previousItem.createdAt) : null;
        const thisDay = Formatter.formatTimestampDate(version.createdAt);
        if (previousDay === thisDay) {
          return Formatter.formatTimestampTime(version.createdAt);
        } else {
          return Formatter.formatTimestampShort(version.createdAt);
        }
      }
    }

    renderVersions(shouldFilterLastSavedVersion: boolean): Node {
      if (this.props.versions.length > 1) {
        return (
          <div>
            <SidebarButton
              selected={!this.state.selectedMenuItem}
              onClick={this.onClickMenuItem.bind(this, null)}
            >
              {this.nameForVersion(this.props.currentGroup, 0, null, shouldFilterLastSavedVersion)}
            </SidebarButton>
            {this.props.versions.map((version: BehaviorGroup, index: number): ?Element<SidebarButton> => {
              if (index === 0) {
                return null;
              } else if (index === 1 && shouldFilterLastSavedVersion) {
                return null;
              } else {
                const key = `version${index}`;
                const previousItem = this.props.versions[index - 1];
                return (
                  <SidebarButton
                    key={key}
                    selected={this.state.selectedMenuItem === key}
                    onClick={this.onClickMenuItem.bind(this, key)}
                  >
                    {this.nameForVersion(version, index, previousItem, shouldFilterLastSavedVersion)}
                  </SidebarButton>
                );
              }
            })}
          </div>);
      } else {
        return (
          <div className="phxl mobile-phl pulse type-disabled">Loading versions…</div>
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

    setDiffDirectionFromSelected(): void {
      this.setState({
        diffFromSelectedToCurrent: true
      });
    }

    setDiffDirectionFromCurrent(): void {
      this.setState({
        diffFromSelectedToCurrent: false
      });
    }

    renderCurrentVersionInfo(shouldFilterLastSavedVersion: boolean): ElementType {
      const lastSavedVersion = this.getLastSavedVersion();
      if (lastSavedVersion && shouldFilterLastSavedVersion) {
        return (
          <BehaviorGroupSaveInfo
            group={lastSavedVersion}
            currentUserId={this.props.currentUserId}
            isLastSavedVersion={true}
          />
        );
      } else if (lastSavedVersion) {
        return (
          <span>You have made unsaved changes to this skill.</span>
        );
      } else {
        return (
          <span className="pulse">Loading…</span>
        );
      }
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

    renderSelectedVersion(shouldFilterLastSavedVersion: boolean): ElementType {
      const versionIndex = this.getSelectedVersionIndex();
      const selectedVersion = this.getSelectedVersion(versionIndex);
      const isLastSaved = versionIndex === 1;
      const isCurrent = versionIndex === 0;
      const diff = this.getDiffForSelectedVersion(selectedVersion);
      console.log(diff);
      return (
        <div className="container container-wide pvxl">
          <div>
            {isCurrent ? this.renderCurrentVersionInfo(shouldFilterLastSavedVersion) : (
              <BehaviorGroupSaveInfo
                group={selectedVersion}
                currentUserId={this.props.currentUserId}
                isLastSavedVersion={isLastSaved}
              />
            )}
          </div>

          {selectedVersion && versionIndex > 0 ? (
            <div>
              <h4>Differences</h4>

              {diff ? (
                <div className="mbm">
                  <ToggleGroup>
                    <ToggleGroup.Item
                      activeWhen={this.state.diffFromSelectedToCurrent}
                      label="What changed since this version"
                      onClick={this.setDiffDirectionFromSelected}
                    />
                    <ToggleGroup.Item
                      activeWhen={!this.state.diffFromSelectedToCurrent}
                      label="What would change if you reverted"
                      onClick={this.setDiffDirectionFromCurrent}
                    />
                  </ToggleGroup>
                </div>
              ) : null}
              {this.renderDiff(diff)}
            </div>
          ) : null}
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

    render(): ElementType {
      const lastSavedVersion = this.getLastSavedVersion();
      const shouldFilterLastSavedVersion = Boolean(lastSavedVersion && lastSavedVersion.isIdenticalTo(this.props.currentGroup));
      return (
        <div className="flex-row-cascade pbxxxxl">
          <div className="bg-white container container-wide pvm border-bottom border-light">

            <Button className="button-raw" onClick={this.props.onClearActivePanel}>{this.props.currentGroup.getName()}</Button>
            <span className="mhs type-weak">→</span>
            <span>Skill versions</span>

          </div>
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide phn">
              <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
                <div className="column column-one-quarter flex-column mobile-column-full ptxl phn bg-white border-right border-light">

                  <h5 className="mtn phxl mobile-phl">Versions</h5>

                  <div className="type-s">
                    {this.renderVersions(shouldFilterLastSavedVersion)}
                  </div>
                </div>
                <div className="column mobile-column-full pbxxxxl column-three-quarters flex-column bg-lightest">
                  {this.renderSelectedVersion(shouldFilterLastSavedVersion)}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
  }

  return VersionBrowser;
});
