define(function(require) {
  var React = require('react'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    TimeZoneSelector = require('./time_zone_selector');

  return React.createClass({
    displayName: 'TeamTimeZone',
    propTypes: {
      onSetTimeZone: React.PropTypes.func.isRequired,
      isSaving: React.PropTypes.bool,
      error: React.PropTypes.string
    },

    getInitialState: function() {
      return {
        timeZoneId: "",
        timeZoneName: ""
      };
    },

    onChange: function(timeZoneId, timeZoneName) {
      this.setState({
        timeZoneId: timeZoneId,
        timeZoneName: timeZoneName
      });
    },

    setTimeZone: function() {
      this.props.onSetTimeZone(this.state.timeZoneId, this.state.timeZoneName);
    },

    render: function() {
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

            <TimeZoneSelector onChange={this.onChange} />

            <div className="mvl">
              <DynamicLabelButton
                className="button-primary mrm"
                onClick={this.setTimeZone}
                disabledWhen={this.props.isSaving || !this.state.timeZoneId}
                labels={[{
                  text: "Set team time zone",
                  displayWhen: !this.props.isSaving
                }, {
                  text: "Saving…",
                  displayWhen: this.props.isSaving
                }]}
              />
              {this.props.error ? (
                <span className="align-button type-italic type-pink fade-in">— {this.props.error}</span>
              ) : null}
            </div>
          </div>
        </div>
      );
    }
  });
});
