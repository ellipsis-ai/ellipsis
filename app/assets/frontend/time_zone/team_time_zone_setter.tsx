import * as React from 'react';
import {DataRequest} from '../lib/data_request';
import Button from '../form/button';
import DynamicLabelButton from '../form/dynamic_label_button';
import TimeZoneSelector from './time_zone_selector';
import autobind from "../lib/autobind";

interface TeamTimeZoneData {
  tzName: string,
  formattedName?: Option<string>,
  currentOffset: number
}

interface Props {
  csrfToken: string
  teamId: string
  onSave: (tzName: string, formattedName: string, newOffset: number) => void
  onCancel?: () => void
  teamTimeZone?: Option<string>
}

interface State {
  timeZoneId: string,
  timeZoneName: string,
  isSaving: boolean,
  justSaved: boolean,
  error: Option<string>
}

class TeamTimeZoneSetter extends React.Component<Props, State> {
    timeZoneSelector: Option<TimeZoneSelector>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        timeZoneId: "",
        timeZoneName: "",
        isSaving: false,
        justSaved: false,
        error: null
      };
    }

    onChange(timeZoneId: string, timeZoneName: string) {
      this.setState({
        timeZoneId: timeZoneId,
        timeZoneName: timeZoneName
      });
    }

    isTeamTimeZoneData(json: any): json is TeamTimeZoneData {
      return typeof json.tzName === "string";
    }

    setTimeZone(): void {
      const newTz = this.state.timeZoneId;
      const displayName = this.state.timeZoneName;
      this.setState({
        isSaving: true,
        error: null
      }, () => {
        const url = jsRoutes.controllers.ApplicationController.setTeamTimeZone().url;
        DataRequest
          .jsonPost(url, {
            tzName: newTz,
            teamId: this.props.teamId
          }, this.props.csrfToken)
          .then((json: TeamTimeZoneData | { message?: string }) => {
            if (this.isTeamTimeZoneData(json)) {
              this.setState({
                isSaving: false,
                justSaved: true
              });
              this.props.onSave(json.tzName, json.formattedName || displayName, json.currentOffset);
            } else {
              throw new Error(json.message || "");
            }
          })
          .catch((err) => {
            this.setState({
              isSaving: false,
              error: `An error occurred while saving${err.message ? ` (${err.message})` : ""}. Please try again.`
            });
          });
      });
    }

    focus(): void {
      if (this.timeZoneSelector) {
        this.timeZoneSelector.focus();
      }
    }

    renderStatus() {
      if (this.state.error) {
        return (
          <span className="align-button type-italic type-pink fade-in">— {this.state.error}</span>
        );
      } else if (this.state.justSaved) {
        return (
          <span className="align-button type-green type-bold fade-in">— Team time zone updated</span>
        );
      } else {
        return null;
      }
    }

    render() {
      return (
        <div>

            <TimeZoneSelector
              ref={(el) => this.timeZoneSelector = el}
              onChange={this.onChange}
              defaultTimeZone={this.props.teamTimeZone}
              resetWithNewDefault={true}
            />

            <div className="mvl">
              <DynamicLabelButton
                className="button-primary mrm"
                onClick={this.setTimeZone}
                disabledWhen={this.state.isSaving || !this.state.timeZoneId}
                labels={[{
                  text: "Save team time zone",
                  displayWhen: !this.state.isSaving
                }, {
                  text: "Saving…",
                  displayWhen: this.state.isSaving
                }]}
              />
              {this.props.onCancel ? (
                <Button onClick={this.props.onCancel} className="mrm">Cancel</Button>
              ) : null}
              {this.renderStatus()}
            </div>

        </div>
      );
    }
}

export default TeamTimeZoneSetter;
