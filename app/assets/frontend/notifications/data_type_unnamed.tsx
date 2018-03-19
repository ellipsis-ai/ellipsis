import * as React from 'react';
import DataTypeUnnamedNotificationData from "../models/notifications/data_type_unnamed_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<DataTypeUnnamedNotificationData>
}

class NotificationForDataTypeUnnamed extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    render() {
      const count = this.props.details.length;
      if (count === 1) {
        return (
          <span>
            <span>Data types require a name.</span>
            <button type="button"
              className="button-raw link button-s mhxs"
              onClick={this.props.details[0].onClick}
            >Edit name</button>
          </span>
        );
      } else {
        return (
          <span>
            <span>Data types require a name.</span>
            {this.props.details.map((ea, index) => (
              <span key={`detail${index}`}>
                <button
                  type="button"
                  className="button-raw link button-s mhxs"
                  onClick={ea.onClick}
                >Edit name {index + 1}</button>
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

export default NotificationForDataTypeUnnamed;

