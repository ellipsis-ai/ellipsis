/* global RegionalSettingsConfiguration:false */
import 'core-js';
import 'whatwg-fetch';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import RegionalSettings from './index';
import Page from '../../../javascripts/shared_ui/page';
import autobind from '../../../javascripts/lib/autobind';

class RegionalSettingsLoader extends React.Component {
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

RegionalSettingsLoader.propTypes = {
  containerId: React.PropTypes.string.isRequired,
  csrfToken: React.PropTypes.string.isRequired,
  isAdmin: React.PropTypes.bool.isRequired,
  teamId: React.PropTypes.string.isRequired,
  teamTimeZone: React.PropTypes.string,
  teamTimeZoneName: React.PropTypes.string,
  teamTimeZoneOffset: React.PropTypes.number
};

ReactDOM.render(
  React.createElement(RegionalSettingsLoader, RegionalSettingsConfiguration),
  document.getElementById(RegionalSettingsConfiguration.containerId)
);
