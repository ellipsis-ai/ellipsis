// @flow
import * as React from 'react';
import type {PageRequiredProps} from "../shared_ui/page";

type Props = {
  children: React.Node
} & PageRequiredProps

class TeamTimeZoneWelcomePage extends React.Component<Props> {
    render() {
      return (
        <div className="bg-white border-bottom border-bottom-thick pvxl">
          <div className="container container-c container-narrow">
            <h2>Welcome to Ellipsis!</h2>

            <h3>Set your team’s time zone</h3>
            <p>
              Before you get started, pick a default time zone for your team.
            </p>

            <p>
              This will be used when Ellipsis displays dates and times to a group, or whenever a time of day mentioned
              isn’t otherwise obvious.
            </p>

            {this.props.children}
          </div>

          {this.props.onRenderFooter()}
        </div>
      );
    }
}

export default TeamTimeZoneWelcomePage;
