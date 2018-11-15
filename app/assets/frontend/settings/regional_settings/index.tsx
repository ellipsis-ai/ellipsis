import {PageRequiredProps} from '../../shared_ui/page';
import * as React from 'react';
import * as moment from 'moment';
import Collapsible from '../../shared_ui/collapsible';

import Button from '../../form/button';
import SettingsPage from '../../shared_ui/settings_page';
import TeamTimeZoneSetter from '../../time_zone/team_time_zone_setter';
import Page from '../../shared_ui/page';
import autobind from '../../lib/autobind';

type RegionalSettingsProps = {
  csrfToken: string,
  isAdmin: boolean,
  teamId: string,
  onSaveTimeZone: (tzName: string, formattedName: string, newOffset: number) => void,
  teamTimeZone: Option<string>,
  teamTimeZoneName: Option<string>,
  teamTimeZoneOffset: Option<number>
}

type Props = RegionalSettingsProps & PageRequiredProps

type State = {
  currentTime: string;
}

class RegionalSettings extends React.Component<Props, State> {
  teamTimeZoneSetter: Option<TeamTimeZoneSetter>;
  static defaultProps: PageRequiredProps;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        currentTime: this.getCurrentTime()
      };
    }

    componentDidMount(): void {
      setInterval(this.updateTime, 1000);
    }

    updateTime(): void {
      if (!document.hidden) {
        this.setState({
          currentTime: this.getCurrentTime()
        });
      }
    }

    getCurrentTime(): string {
      const time = moment().utc();
      if (typeof this.props.teamTimeZoneOffset === "number") {
        time.add(this.props.teamTimeZoneOffset, 'seconds');
      }
      return time.format('h:mm:ss A');
    }

    toggleTimeZoneSetter(): void {
      this.props.onToggleActivePanel('timeZoneSetter', true, () => {
        if (this.teamTimeZoneSetter) {
          this.teamTimeZoneSetter.focus();
        }
      });
    }

    onSaveTimeZone(tzName: string, formattedName: string, newOffset: number): void {
      this.props.onSaveTimeZone(tzName, formattedName, newOffset);
      this.props.onClearActivePanel();
    }

    renderCurrentTime() {
      if (typeof this.props.teamTimeZoneOffset === "number") {
        return (
          <span> — {this.state.currentTime}</span>
        );
      } else {
        return null;
      }
    }

    renderSetterPanel() {
      return (
        <Collapsible revealWhen={this.props.activePanelName === "timeZoneSetter"}>
          <div className="box-action phn">
            <div className="container">
              <div className="columns">
                <div className="column column-page-sidebar">
                  <h4 className="mtn type-weak">Set new team time zone</h4>
                </div>
                <div className="column column-page-main">

                  <p>Search for a city to set a new team time zone.</p>

                  <TeamTimeZoneSetter
                    ref={(el) => this.teamTimeZoneSetter = el}
                    csrfToken={this.props.csrfToken}
                    teamId={this.props.teamId}
                    onSave={this.onSaveTimeZone}
                    teamTimeZone={this.props.teamTimeZone}
                    onCancel={this.props.onClearActivePanel}
                  />
                </div>
              </div>
            </div>
          </div>
        </Collapsible>
      );
    }

    render() {
      return (
        <SettingsPage teamId={this.props.teamId} activePage={"regionalSettings"} isAdmin={this.props.isAdmin}>

          <h5>Team time zone</h5>

          <div className="mbs">
            {this.props.teamTimeZoneName ? (
              <span className="type-bold">{this.props.teamTimeZoneName}</span>
            ) : (
              <span className="type-italic type-disabled">No time zone chosen</span>
            )}
            {this.renderCurrentTime()}
          </div>
          <div>
            <Button className="button-s" onClick={this.toggleTimeZoneSetter}>Change time zone</Button>
          </div>

          {this.props.onRenderFooter(this.renderSetterPanel())}

        </SettingsPage>
      );
    }
  }

RegionalSettings.defaultProps = Page.requiredPropDefaults();

export default RegionalSettings;
