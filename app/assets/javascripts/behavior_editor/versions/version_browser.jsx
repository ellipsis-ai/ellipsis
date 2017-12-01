// @flow
import type {Node, Element, ElementType} from 'react';
define(function(require: (string) => *): React.ElementType {
  const React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    BehaviorGroupSaveInfo = require('../behavior_group_save_info'),
    Button = require('../../form/button'),
    Formatter = require('../../lib/formatter'),
    SidebarButton = require('../../form/sidebar_button'),
    autobind = require('../../lib/autobind');

  type Props = {
    currentGroup: BehaviorGroup,
    currentUserId: string,
    versions: Array<BehaviorGroup>,
    onClearActivePanel: () => void,
    onRestoreClick: (index: number) => void
  };

  type State = {
    selectedMenuItem: ?string
  }

  class VersionBrowser extends React.Component<Props> {
    props: Props;
    state: State;

    constructor(props: Props): void {
      super(props);
      autobind(this);
      this.state = {
        selectedMenuItem: null
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
          return "Current version (no changes)";
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

    renderSelectedVersion(shouldFilterLastSavedVersion: boolean): ElementType {
      const versionIndex = this.getSelectedVersionIndex();
      const version = this.getSelectedVersion(versionIndex);
      const isLastSaved = versionIndex === 1;
      const isCurrent = versionIndex === 0;
      return (
        <div className="container container-wide pvxl">
          <h5 className="mtn">Version info</h5>
          <div>
            {isCurrent ? this.renderCurrentVersionInfo(shouldFilterLastSavedVersion) : (
              <BehaviorGroupSaveInfo
                group={version}
                currentUserId={this.props.currentUserId}
                isLastSavedVersion={isLastSaved}
              />
            )}
          </div>
        </div>
      );
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
