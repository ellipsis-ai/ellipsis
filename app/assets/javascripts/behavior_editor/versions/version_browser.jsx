// @flow
define(function(require) {
  const React = require('react'),
    BehaviorGroup = require('../../models/behavior_group'),
    Button = require('../../form/button'),
    Formatter = require('../../lib/formatter'),
    SidebarButton = require('../../form/sidebar_button'),
    autobind = require('../../lib/autobind');

  type Props = {
    currentGroup: BehaviorGroup,
    versions: Array<BehaviorGroup>,
    onClearActivePanel: () => void
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
        selectedMenuItem: this.props.versions[0] ? "version0" : null
      };
    }

    onClickMenuItem(key: string): void {
      this.setState({
        selectedMenuItem: key
      });
    }

    nameForVersion(version: BehaviorGroup, index: number): string {
      if (index === 0) {
        if (version.isIdenticalTo(this.props.currentGroup)) {
          return "Last saved version (no changes)";
        } else {
          return "Last saved version (before current changes)";
        }
      } else {
        return Formatter.formatTimestampRelativeIfRecent(version.createdAt);
      }
    }

    renderVersions(): React.Element {
      if (this.props.versions.length > 1) {
        return this.props.versions.slice(1).map((version, index) => {
          const key = `version${index}`;
          return (
            <SidebarButton
              key={key}
              selected={this.state.selectedMenuItem === key}
              onClick={this.onClickMenuItem.bind(this, key)}
              className="type-s"
            >
              {this.nameForVersion(version, index)}
            </SidebarButton>
          );
        });
      } else {
        return (
          <div className="phxl mobile-phl pulse type-disabled">Loading versions…</div>
        );
      }
    }

    render(): React.Element {
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

                  {this.renderVersions()}
                </div>
                <div className="column mobile-column-full pbxxxxl column-three-quarters flex-column bg-lightest">

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
