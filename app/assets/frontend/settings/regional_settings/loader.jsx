// @flow
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import RegionalSettings from './index';
import PageComponent from '../../../javascripts/shared_ui/page';
const Page: any = PageComponent;
import autobind from '../../../javascripts/lib/autobind';

type Props = {
  containerId: string,
  csrfToken: string,
  isAdmin: boolean,
  teamId: string,
  teamTimeZone: ?string,
  teamTimeZoneName: ?string,
  teamTimeZoneOffset: ?number
};

declare var RegionalSettingsConfiguration: Props;

type State = {
  teamTimeZone: ?string,
  teamTimeZoneName: ?string,
  teamTimeZoneOffset: ?number
}

class RegionalSettingsLoader extends React.PureComponent<Props, State> {
        constructor(props) {
          super(props);
          autobind(this);
          this.state = {
            teamTimeZone: this.props.teamTimeZone,
            teamTimeZoneName: this.props.teamTimeZoneName,
            teamTimeZoneOffset: this.props.teamTimeZoneOffset
          };
        }

        onSaveTimeZone(newTz, newTzName, newOffset) {
          this.setState({
            teamTimeZone: newTz,
            teamTimeZoneName: newTzName,
            teamTimeZoneOffset: newOffset
          });
        }

        render() {
          return (
            <Page csrfToken={this.props.csrfToken}>
              <RegionalSettings
                csrfToken={this.props.csrfToken}
                isAdmin={this.props.isAdmin}
                teamId={this.props.teamId}
                teamTimeZone={this.state.teamTimeZone}
                teamTimeZoneName={this.state.teamTimeZoneName}
                teamTimeZoneOffset={this.state.teamTimeZoneOffset}
                onSaveTimeZone={this.onSaveTimeZone}
              />
            </Page>
          );
        }
}

const container = document.getElementById(RegionalSettingsConfiguration.containerId);

if (container) {
  ReactDOM.render((
    <RegionalSettingsLoader {...RegionalSettingsConfiguration} />
  ), container);
}
