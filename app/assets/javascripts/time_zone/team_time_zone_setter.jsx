define(function(require) {
  var React = require('react'),
    DataRequest = require('../lib/data_request'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    TimeZoneSelector = require('./time_zone_selector');

  const TeamTimeZoneSetter = React.createClass({
    displayName: '',
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      onSave: React.PropTypes.func.isRequired,
      teamTimeZone: React.PropTypes.string
    },

    getInitialState: function() {
      return {
        timeZoneId: "",
        timeZoneName: "",
        isSaving: false,
        justSaved: false,
        error: null
      };
    },

    onChange: function(timeZoneId, timeZoneName) {
      this.setState({
        timeZoneId: timeZoneId,
        timeZoneName: timeZoneName
      });
    },

    setTimeZone: function() {
      const newTz = this.state.timeZoneId;
      const displayName = this.state.timeZoneName;
      this.setState({
        isSaving: true,
        error: null
      }, () => {
        const url = jsRoutes.controllers.ApplicationController.setTeamTimeZone().url;
        DataRequest
          .jsonPost(url, {
            tzName: newTz,
            teamId: this.props.teamId
          }, this.props.csrfToken)
          .then((json) => {
            if (json.tzName) {
              this.setState({
                isSaving: false,
                justSaved: true
              });
              setTimeout(() => {
                this.setState({
                  justSaved: false
                });
              }, 5000);
              this.props.onSave(json.tzName, json.formattedName || displayName, json.currentOffset);
            } else {
              throw new Error(json.message || "");
            }
          })
          .catch((err) => {
            this.setState({
              isSaving: false,
              error: `An error occurred while saving${err.message ? ` (${err.message})` : ""}. Please try again.`
            });
          });
      });
    },

    hasSelectedNewTimeZone: function(timeZoneId) {
      return timeZoneId && timeZoneId !== this.props.teamTimeZone;
    },

    renderStatus: function() {
      if (this.state.error) {
        return (
          <span className="align-button type-italic type-pink fade-in">— {this.state.error}</span>
        );
      } else if (this.state.justSaved) {
        return (
          <span className="align-button type-green type-bold fade-in">— Team time zone updated</span>
        );
      }
    },

    render: function() {
      return (
        <div>

            <TimeZoneSelector
              onChange={this.onChange}
              defaultTimeZone={this.props.teamTimeZone}
              resetWithNewDefault={true}
            />

            <div className="mvl">
              <DynamicLabelButton
                className="button-primary mrm"
                onClick={this.setTimeZone}
                disabledWhen={this.state.isSaving || !this.hasSelectedNewTimeZone(this.state.timeZoneId)}
                labels={[{
                  text: "Set team time zone",
                  displayWhen: !this.state.isSaving
                }, {
                  text: "Saving…",
                  displayWhen: this.state.isSaving
                }]}
              />
              {this.renderStatus()}
            </div>

        </div>
      );
    }
  });

  return TeamTimeZoneSetter;
});
