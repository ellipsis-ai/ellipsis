import * as React from 'react';
import Recurrence, {RecurrenceJson} from '../models/recurrence';
import MinutelyRecurrenceEditor from './minutely_recurrence_editor';
import HourlyRecurrenceEditor from './hourly_recurrence_editor';
import DailyRecurrenceEditor from './daily_recurrence_editor';
import WeeklyRecurrenceEditor from './weekly_recurrence_editor';
import MonthlyRecurrenceEditor from './monthly_recurrence_editor';
import YearlyRecurrenceEditor from './yearly_recurrence_editor';
import RecurrenceIntervalEditor from "./recurrence_interval_editor";
import autobind from "../lib/autobind";
import Formatter, {Timestamp} from "../lib/formatter";
import * as debounce from "javascript-debounce";
import {DataRequest} from "../lib/data_request";

export interface RecurrenceEditorProps {
  recurrence: Recurrence,
  onChange: (recurrence: Recurrence) => void,
}

export interface RecurrenceEditorTimeZoneProps {
  teamTimeZone: string,
  teamTimeZoneName: string
}

interface ValidatedRecurrenceJson {
  recurrence: RecurrenceJson,
  nextRuns: Array<Timestamp>
}

type Props = RecurrenceEditorProps & RecurrenceEditorTimeZoneProps & {
  csrfToken: string
}

interface State {
  validated: Option<ValidatedRecurrenceJson>,
  isValidating: boolean,
  error: Option<string>
}

class RecurrenceEditor extends React.Component<Props, State> {
    validateRecurrence: (recurrence: Recurrence) => void;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        validated: null,
        isValidating: true,
        error: null
      };
      this.validateRecurrence = debounce(this._validateRecurrence, 500);
    }

    componentDidMount() {
      this.validateRecurrence(this.props.recurrence);
    }

    componentWillReceiveProps(nextProps: Props) {
      if (nextProps.recurrence !== this.props.recurrence) {
        this.setState({
          isValidating: true,
          error: null
        }, () => {
          this.validateRecurrence(nextProps.recurrence);
        });
      }
    }

    _validateRecurrence(recurrence: Recurrence): void {
      const url = jsRoutes.controllers.ScheduledActionsController.validateRecurrence().url;
      DataRequest.jsonPost(url, recurrence, this.props.csrfToken)
        .then((data: ValidatedRecurrenceJson) => {
          this.setState({
            validated: data,
            isValidating: false
          });
        }).catch(() => {
          this.setState({
            validated: null,
            isValidating: false,
            error: "This is not a valid schedule."
          });
        });
    }

    renderRecurrenceEditorForType() {
      if (this.props.recurrence.typeName === "yearly") {
        return (
          <YearlyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.props.recurrence.typeName.indexOf("monthly") === 0) {
        return (
          <MonthlyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.props.recurrence.typeName === "weekly") {
        return (
          <WeeklyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.props.recurrence.typeName === "daily") {
        return (
          <DailyRecurrenceEditor recurrence={this.props.recurrence}
            onChange={this.props.onChange}
            teamTimeZone={this.props.teamTimeZone}
            teamTimeZoneName={this.props.teamTimeZoneName}
          />
        );
      } else if (this.props.recurrence.typeName === "hourly") {
        return (
          <HourlyRecurrenceEditor recurrence={this.props.recurrence} onChange={this.props.onChange}/>
        );
      } else { /* this.props.recurrence.typeName === "minutely" or any future unknown type */
        return (
          <MinutelyRecurrenceEditor recurrence={this.props.recurrence} onChange={this.props.onChange}/>
        );
      }
    }

    renderNextRuns() {
      if (this.state.isValidating) {
        return (
          <div className="pulse">Loading…</div>
        );
      } else if (this.state.validated) {
        const firstRun = this.state.validated.nextRuns[0];
        const secondRun = this.state.validated.nextRuns[1];
        return (
          <div>
            <span>{firstRun ? Formatter.formatTimestampLong(firstRun) : "Never"}</span>
            {secondRun ? (
              <span>
                <span className="mhs type-disabled">·</span>
                <span>{Formatter.formatTimestampLong(secondRun)}</span>
              </span>
            ) : null}
          </div>
        );
      } else if (this.state.error) {
        return (
          <div className="type-pink">{this.state.error}</div>
        );
      } else {
        return (
          <div className="type-italic type-disabled">Unknown</div>
        );
      }
    }

    render() {
      return (
        <div>
          <div className="mvm">
            <RecurrenceIntervalEditor
              recurrence={this.props.recurrence}
              onChange={this.props.onChange}
              teamTimeZone={this.props.teamTimeZone}
              teamTimeZoneName={this.props.teamTimeZoneName}
            />
          </div>

          <div className="mvm">
            {this.renderRecurrenceEditorForType()}
          </div>

          <div className="mvm border border-blue bg-blue-lighter pam">
            <div className="type-label type-weak mbxs">When this will run next:</div>
            <div className="type-s">
              {this.renderNextRuns()}
            </div>
          </div>
        </div>
      );
    }
}

export default RecurrenceEditor;
