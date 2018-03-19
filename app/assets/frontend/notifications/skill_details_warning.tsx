import * as React from 'react';
import SkillDetailsWarningNotificationData from "../models/notifications/skill_details_warning_notification_data";
import autobind from '../lib/autobind';

interface Props {
  details: Array<SkillDetailsWarningNotificationData>
}

class NotificationForSkillDetailsWarning extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

    render() {
      const detail = this.props.details.find((ea) => ea.type === "no_skill_name");
      return (
        <span>
          <span className="type-label">Warning: </span>
          <span className="mrs">This skill is untitled. Add a title to identify it to your team.</span>
          {detail ? (
            <button className="button-s button-shrink" type="button" onClick={detail.onClick}>Edit skill details</button>
          ) : null}
        </span>
      );
    }
}

export default NotificationForSkillDetailsWarning;
