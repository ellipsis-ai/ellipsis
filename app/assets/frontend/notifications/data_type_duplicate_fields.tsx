import * as React from 'react';
import DataTypeDuplicateFieldsNotificationData from "../models/notifications/data_type_duplicate_fields_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<DataTypeDuplicateFieldsNotificationData>
}

class NotificationForDataTypeDuplicateFields extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    render() {
      const count = this.props.details.length;
      if (count === 1) {
        return (
          <span>
            <span>Data type field names must be unique.</span>
            <button type="button"
              className="button-raw link button-s mhxs"
              onClick={this.props.details[0].onClick}
            >Edit duplicate field on {this.props.details[0].name || "data type"}</button>
          </span>
        );
      } else {
        return (
          <span>
            <span>Data type field names must be unique.</span>
            {this.props.details.map((ea, index) => (
              <span key={`detail${index}`}>
                <button
                  type="button"
                  className="button-raw link button-s mhxs"
                  onClick={ea.onClick}
                >Edit duplicate field on {ea.name || `data type ${index + 1}`}</button>
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

export default NotificationForDataTypeDuplicateFields;

