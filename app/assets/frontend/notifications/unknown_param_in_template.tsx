import * as React from 'react';
import UnknownParamInTemplateNotificationData from "../models/notifications/unknown_param_in_template_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<UnknownParamInTemplateNotificationData>
}

class NotificationForUnknownParamInTemplate extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    render() {
      var numParams = this.props.details.length;
      if (numParams === 1) {
        let detail = this.props.details[0];
        return (
          <span>The response contains an unknown variable name: <code>{detail.name}</code></span>
        );
      } else {
        return (
          <span>
            <span>The response contains unknown variable names: </span>
            {this.props.details.map((detail, index) => (
              <span key={`unknownParamName${index}`}>
                <code className="mhxs type-bold">{detail.name}</code>
                <span className="type-weak">{index + 1 < numParams ? " · " : ""}</span>
              </span>
            ))}
          </span>
        );
      }
    }
}

export default NotificationForUnknownParamInTemplate;
