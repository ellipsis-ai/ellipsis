import * as React from 'react';
import DataRequest from '../lib/data_request';
import Button from '../form/button';
import DynamicLabelButton from '../form/dynamic_label_button';
import TimeZoneSelector from './time_zone_selector';

const TeamTimeZoneSetter = React.createClass({
    propTypes: {
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.isRequired,
      onSave: React.PropTypes.func.isRequired,
      onCancel: React.PropTypes.func,
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

    focus: function() {
      if (this.timeZoneSelector) {
        this.timeZoneSelector.focus();
      }
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
              ref={(el) => this.timeZoneSelector = el}
              onChange={this.onChange}
              defaultTimeZone={this.props.teamTimeZone}
              resetWithNewDefault={true}
            />

            <div className="mvl">
              <DynamicLabelButton
                className="button-primary mrm"
                onClick={this.setTimeZone}
                disabledWhen={this.state.isSaving || !this.state.timeZoneId}
                labels={[{
                  text: "Save team time zone",
                  displayWhen: !this.state.isSaving
                }, {
                  text: "Saving…",
                  displayWhen: this.state.isSaving
                }]}
              />
              {this.props.onCancel ? (
                <Button onClick={this.props.onCancel} className="mrm">Cancel</Button>
              ) : null}
              {this.renderStatus()}
            </div>

        </div>
      );
    }
  });

export default TeamTimeZoneSetter;
