import * as React from 'react';
import DataTypeMissingFieldsNotificationData from "../models/notifications/data_type_missing_fields_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<DataTypeMissingFieldsNotificationData>
}

class NotificationForDataTypeMissingFields extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
      const count = this.props.details.length;
      if (count === 1) {
        return (
          <span>
            <span>Each data type should have at least one text field.</span>
            <button type="button"
              className="button-raw link button-s mhxs"
              onClick={this.props.details[0].onClick}
            >Add field to {this.props.details[0].name || "data type"}</button>
          </span>
        );
      } else {
        return (
          <span>
            <span>Each data type should have at least one text field.</span>
            {this.props.details.map((ea, index) => (
              <span key={`detail${index}`}>
                <button
                  type="button"
                  className="button-raw link button-s mhxs"
                  onClick={ea.onClick}
                >Add field to {ea.name || `data type ${index + 1}`}</button>
                {index + 1 < this.props.details.length ? (
                  <span className="mhxs type-weak">·</span>
                ) : null}
              </span>
            ))}
          </span>
        );
      }
  }
}

export default NotificationForDataTypeMissingFields;

