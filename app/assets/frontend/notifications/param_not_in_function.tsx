import * as React from 'react';
import ParamNotInFunctionNotificationData from "../models/notifications/param_not_in_function_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<ParamNotInFunctionNotificationData>
}

class NotificationForParamNotInFunction extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    onClick(): void {
      this.props.details.forEach((detail) => {
        detail.onClick();
      });
    }

    render() {
      var numParams = this.props.details.length;
      if (numParams === 1) {
        let detail = this.props.details[0];
        return (
          <span>
            <span>You’ve specified a new input labeled </span>
            <span className="box-code-example mhxs">{detail.name}</span>
            <span>in this trigger. Add it to </span>
            <span>use it in code or in the response: </span>
            <button type="button" className="button-s button-shrink mhxs" onClick={this.onClick}>
              Add input
            </button>
          </span>
        );
      } else {
        return (
          <span>
            <span>You’ve specified {numParams} new inputs in this trigger. Add them to </span>
            <span>use them in code or in the response: </span>
            <button type="button" className="button-s button-shrink mhxs" onClick={this.onClick}>
              Add inputs
            </button>
          </span>
        );
      }
    }
}

export default NotificationForParamNotInFunction;
