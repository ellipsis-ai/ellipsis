define(function(require) {
  const React = require('react'),
    SettingsPage = require('../shared_ui/settings_page'),
    TimeZoneSelector = require('../time_zone/time_zone_selector'),
    autobind = require('../lib/autobind');

  class RegionalSettings extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        timeZoneId: "",
        timeZoneName: ""
      };
    }

    onChangeTimeZone(timeZoneId, timeZoneName) {
      this.setState({
        timeZoneId: timeZoneId,
        timeZoneName: timeZoneName
      });
    }

    render() {
      return (
        <SettingsPage teamId={this.props.teamId} activePage={"regionalSettings"} header={"Regional settings"}>

          <h4>Team time zone</h4>
          <TimeZoneSelector onChange={this.onChangeTimeZone} defaultTimeZone={this.props.teamTimeZone} />
        </SettingsPage>
      );
    }
  }

  RegionalSettings.propTypes = {
    csrfToken: React.PropTypes.string.isRequired,
    teamId: React.PropTypes.string.isRequired,
    teamTimeZone: React.PropTypes.string
  };

  return RegionalSettings;
});
