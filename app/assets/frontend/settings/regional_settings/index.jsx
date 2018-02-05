// @flow
import type {PageRequiredProps} from '../../../javascripts/shared_ui/page';
import * as React from 'react';
import moment from 'moment';
import Collapsible from '../../../javascripts/shared_ui/collapsible';

// TODO: Remove the "any" types here once the required modules are converted to ES6
import ButtonComponent from '../../../javascripts/form/button';
const Button: any = ButtonComponent;
import SettingsPage from '../../../javascripts/shared_ui/settings_page';
import TeamTimeZoneSetterComponent from '../../../javascripts/time_zone/team_time_zone_setter';
const TeamTimeZoneSetter: any = TeamTimeZoneSetterComponent;
import PageComponent from '../../../javascripts/shared_ui/page';
const Page: any = PageComponent;
import autobind from '../../../javascripts/lib/autobind';

type RegionalSettingsProps = {
  csrfToken: string,
  isAdmin: boolean,
  teamId: string,
  onSaveTimeZone: (tzName: string, formattedName: string, newOffset: number) => void,
  teamTimeZone?: string,
  teamTimeZoneName?: string,
  teamTimeZoneOffset?: number
}

type Props = RegionalSettingsProps & PageRequiredProps

type State = {
  currentTime: string;
}

class RegionalSettings extends React.Component<Props, State> {
  props: Props;
  teamTimeZoneSetter: ?TeamTimeZoneSetter;
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
      if (this.props.teamTimeZoneOffset) {
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

    renderCurrentTime(): ?React.Node {
      if (this.props.teamTimeZoneOffset) {
        return (
          <span> — {this.state.currentTime}</span>
        );
      }
    }

    renderSetterPanel(): React.Node {
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

    render(): React.Node {
      return (
        <SettingsPage teamId={this.props.teamId} activePage={"regionalSettings"} header={"Regional settings"} isAdmin={this.props.isAdmin}>

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
