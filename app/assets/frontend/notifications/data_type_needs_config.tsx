import * as React from 'react';
import DataTypeNeedsConfigNotificationData from "../models/notifications/data_type_needs_config_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<DataTypeNeedsConfigNotificationData>
}

class NotificationForDataTypeNeedsConfig extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    renderDetail(detail: DataTypeNeedsConfigNotificationData) {
      return (
        <span>
          <span>The <b>{detail.name}</b> data type needs to be configured.</span>
          <span className="mhxs">
            <button type="button"
                    className="button-raw link button-s"
                    onClick={detail.onClick}>
              Configure it
            </button>
          </span>
        </span>
      );
    }

    render() {
      return this.renderDetail(this.props.details[0]);
    }
}

export default NotificationForDataTypeNeedsConfig;

