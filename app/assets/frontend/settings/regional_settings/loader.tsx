import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import RegionalSettings from './index';
import PageComponent, {PageRequiredProps} from '../../shared_ui/page';
const Page: any = PageComponent;
import autobind from '../../lib/autobind';

type Props = {
  containerId: string,
  csrfToken: string,
  isAdmin: boolean,
  teamId: string,
  teamTimeZone: Option<string>,
  teamTimeZoneName: Option<string>,
  teamTimeZoneOffset: Option<number>
};

declare var RegionalSettingsConfiguration: Props;

type State = {
  teamTimeZone: Option<string>,
  teamTimeZoneName: Option<string>,
  teamTimeZoneOffset: Option<number>
}

class RegionalSettingsLoader extends React.PureComponent<Props, State> {
        constructor(props: Props) {
          super(props);
          autobind(this);
          this.state = {
            teamTimeZone: this.props.teamTimeZone,
            teamTimeZoneName: this.props.teamTimeZoneName,
            teamTimeZoneOffset: this.props.teamTimeZoneOffset
          };
        }

        onSaveTimeZone(newTz: string, newTzName: string, newOffset: number) {
          this.setState({
            teamTimeZone: newTz,
            teamTimeZoneName: newTzName,
            teamTimeZoneOffset: newOffset
          });
        }

        render() {
          return (
            <Page csrfToken={this.props.csrfToken}
              onRender={(pageProps: PageRequiredProps) => (
              <RegionalSettings
                csrfToken={this.props.csrfToken}
                isAdmin={this.props.isAdmin}
                teamId={this.props.teamId}
                teamTimeZone={this.state.teamTimeZone}
                teamTimeZoneName={this.state.teamTimeZoneName}
                teamTimeZoneOffset={this.state.teamTimeZoneOffset}
                onSaveTimeZone={this.onSaveTimeZone}
                {...pageProps}
              />
            )} />
          );
        }
}

const container = document.getElementById(RegionalSettingsConfiguration.containerId);

if (container) {
  ReactDOM.render((
    <RegionalSettingsLoader {...RegionalSettingsConfiguration} />
  ), container);
}
