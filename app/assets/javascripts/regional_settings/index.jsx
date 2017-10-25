define(function(require) {
  const React = require('react'),
    moment = require('moment'),
    Collapsible = require('../shared_ui/collapsible'),
    Button = require('../form/button'),
    SettingsPage = require('../shared_ui/settings_page'),
    TeamTimeZoneSetter = require('../time_zone/team_time_zone_setter'),
    Page = require('../shared_ui/page'),
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

    toggleTimeZoneSetter() {
      this.props.onToggleActivePanel('timeZoneSetter', true, () => {
        if (this.teamTimeZoneSetter) {
          this.teamTimeZoneSetter.focus();
        }
      });
    }

    onSaveTimeZone(tzName, formattedName, newOffset) {
      this.props.onSaveTimeZone(tzName, formattedName, newOffset);
      this.props.onClearActivePanel();
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
            <span className="mrm">{this.renderCurrentTime()}</span>
            <span><Button className="button-s" onClick={this.toggleTimeZoneSetter}>Change</Button></span>
          </p>

          {this.props.onRenderFooter((
            <Collapsible revealWhen={this.props.activePanelName === "timeZoneSetter"}>
              <div className="box-action phn">
                <div className="container">
                  <div className="columns">
                    <div className="column column-page-sidebar">
                      <h4 className="mtn type-weak">Set new team time zone</h4>
                    </div>
                    <div className="column column-page-main">

                      <p>Search for a city to set a new team time zone.</p>

                      <TeamTimeZoneSetter
                        ref={(el) => this.teamTimeZoneSetter = el}
                        csrfToken={this.props.csrfToken}
                        teamId={this.props.teamId}
                        onSave={this.onSaveTimeZone}
                        teamTimeZone={this.props.teamTimeZone}
                        onCancel={this.props.onClearActivePanel}
                      />
                    </div>
                  </div>
                </div>
              </div>
            </Collapsible>
          ))}

        </SettingsPage>
      );
    }
  }

  RegionalSettings.propTypes = Object.assign({}, Page.requiredPropTypes, {
    csrfToken: React.PropTypes.string.isRequired,
    teamId: React.PropTypes.string.isRequired,
    onSaveTimeZone: React.PropTypes.func.isRequired,
    teamTimeZone: React.PropTypes.string,
    teamTimeZoneName: React.PropTypes.string,
    teamTimeZoneOffset: React.PropTypes.number
  });
  RegionalSettings.defaultProps = Page.requiredPropDefaults();

  return RegionalSettings;
});
