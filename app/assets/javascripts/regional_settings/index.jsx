define(function(require) {
  const React = require('react'),
    moment = require('moment'),
    SettingsPage = require('../shared_ui/settings_page'),
    TeamTimeZone = require('../time_zone/team_time_zone'),
    autobind = require('../lib/autobind');

  class RegionalSettings extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
      this.state = {
        currentTime: this.getCurrentTime()
      };
    }

    componentDidMount() {
      setInterval(this.updateTime, 1000);
    }

    updateTime() {
      if (!document.hidden) {
        this.setState({
          currentTime: this.getCurrentTime()
        });
      }
    }

    getCurrentTime() {
      const time = moment().utc();
      if (this.props.teamTimeZoneOffset) {
        time.add(this.props.teamTimeZoneOffset, 'seconds');
      }
      return time;
    }

    renderCurrentTime() {
      if (this.props.teamTimeZoneOffset) {
        return (
          <span> — {this.state.currentTime.format('h:mm:ss A')}</span>
        );
      }
    }

    render() {
      return (
        <SettingsPage teamId={this.props.teamId} activePage={"regionalSettings"} header={"Regional settings"}>

          <h5>Team time zone</h5>

          <p>
            {this.props.teamTimeZoneName ? (
              <span className="type-bold">{this.props.teamTimeZoneName}</span>
            ) : (
              <span className="type-italic type-disabled">No time zone chosen</span>
            )}
            {this.renderCurrentTime()}
          </p>

          <TeamTimeZone
            csrfToken={this.props.csrfToken}
            teamId={this.props.teamId}
            onSave={this.props.onSaveTimeZone}
            teamTimeZone={this.props.teamTimeZone}
          />

        </SettingsPage>
      );
    }
  }

  RegionalSettings.propTypes = {
    csrfToken: React.PropTypes.string.isRequired,
    teamId: React.PropTypes.string.isRequired,
    onSaveTimeZone: React.PropTypes.func.isRequired,
    teamTimeZone: React.PropTypes.string,
    teamTimeZoneName: React.PropTypes.string,
    teamTimeZoneOffset: React.PropTypes.number
  };

  return RegionalSettings;
});
