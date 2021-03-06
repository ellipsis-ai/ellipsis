import * as React from 'react';
import InvalidParamInTriggerNotificationData from "../models/notifications/invalid_param_in_trigger_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<InvalidParamInTriggerNotificationData>
}

class NotificationForInvalidParamInTrigger extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    render() {
      var numParams = this.props.details.length;
      if (numParams === 1) {
        let detail = this.props.details[0];
        return (
          <span>
            <span><code className="type-bold">{`{${detail.name}}`}</code> is not a valid input label. </span>
            <span>Input labels should only include non-accented letters, numbers, dollar signs, or underscores, and can’t have spaces or start with numbers.</span>
          </span>
        );
      } else {
        return (
          <span>
            <span>Input labels can only include non-accented letters, numbers, dollar signs, or underscores, and can’t have spaces or start with numbers. </span>
            <span>These are not valid labels: </span>
            {this.props.details.map((detail, index) => (
              <span key={`invalidParamName${index}`}>
                <code className="mhxs type-bold">{`{${detail.name}}`}</code>
                <span className="type-weak">{index + 1 < numParams ? " · " : ""}</span>
              </span>
            ))}
          </span>
        );
      }
    }
}

export default NotificationForInvalidParamInTrigger;
