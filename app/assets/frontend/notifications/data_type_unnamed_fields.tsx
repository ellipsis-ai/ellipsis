import * as React from 'react';
import DataTypeUnnamedFieldsNotificationData from "../models/notifications/data_type_unnamed_fields_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<DataTypeUnnamedFieldsNotificationData>
}

class NotificationForDataTypeUnnamedFields extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    render() {
      const count = this.props.details.length;
      if (count === 1) {
        return (
          <span>
            <span>Each data type field must have a name.</span>
            <button type="button"
              className="button-raw link button-s mhxs"
              onClick={this.props.details[0].onClick}
            >Edit fields on {this.props.details[0].name || "data type"}</button>
          </span>
        );
      } else {
        return (
          <span>
            <span>Each data type field must have a name.</span>
            {this.props.details.map((ea, index) => (
              <span key={`detail${index}`}>
                <button
                  type="button"
                  className="button-raw link button-s mhxs"
                  onClick={ea.onClick}
                >Edit fields on {ea.name || `data type ${index + 1}`}</button>
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

export default NotificationForDataTypeUnnamedFields;

