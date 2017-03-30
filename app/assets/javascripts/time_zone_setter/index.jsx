define(function(require) {
  var React = require('react'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    Select = require('../form/select'),
    SearchInput = require('../form/search'),
    tzInfo = require('./tz_info');

  return React.createClass({
    displayName: 'TimeZoneSetter',
    propTypes: {
      onSetTimeZone: React.PropTypes.func.isRequired,
      isSaving: React.PropTypes.bool,
      error: React.PropTypes.string
    },

    guessTimeZone: function() {
      let guessed;
      try {
        guessed = Intl.DateTimeFormat().resolvedOptions().timeZone;
      } catch(e) {
        guessed = 'America/New_York';
      }
      return guessed;
    },

    getInitialState: function() {
      var guessedTimeZone = this.guessTimeZone();
      return {
        guessedTimeZone: guessedTimeZone,
        searchText: "",
        selectedTimeZone: guessedTimeZone
      };
    },

    getFilteredTzInfo: function() {
      var searchText = (this.state.searchText || "").trim().toLowerCase();
      if (searchText) {
        return tzInfo.filter((tz) => {
          return tz.name.toLowerCase().includes(searchText) ||
            tz.timeZones.some((tzId) => tzId.toLowerCase().includes(searchText));
        });
      } else {
        return tzInfo;
      }
    },

    updateSelectedTimeZone: function(newValue) {
      this.setState({
        selectedTimeZone: newValue
      });
    },

    updateSearchText: function(newValue) {
      this.setState({
        searchText: newValue
      }, () => {
        var names = this.getFilteredTzInfo();
        if (!names.some((tz) => tz.timeZones.includes(this.state.selectedTimeZone)) && names.length > 0) {
          this.setState({
            selectedTimeZone: names[0].timeZones[0]
          });
        } else if (!names.length) {
          this.setState({
            selectedTimeZone: this.state.guessedTimeZone
          });
        }
      });
    },

    getCurrentDisplayName: function() {
      var targetTz = tzInfo.find((ea) => ea.timeZones.includes(this.state.selectedTimeZone));
      if (targetTz) {
        return targetTz.name;
      } else {
        return this.state.selectedTimeZone.replace(/_/g, ' ');
      }
    },

    setTimeZone: function() {
      this.props.onSetTimeZone(this.state.selectedTimeZone, this.getCurrentDisplayName());
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
              This will be used when Ellipsis displays dates and times to a group, or whenever a time of day mentioned isn’t otherwise obvious.
            </p>

            <div className="mvl">
              <span className="align-button mrm">Selected time zone:</span>
              <Select className="width-15" value={this.state.selectedTimeZone} onChange={this.updateSelectedTimeZone}>
                {tzInfo.map((tz) => (
                  <option key={tz.name} value={tz.timeZones[0]}>{tz.name}</option>
                ))}
              </Select>
            </div>
            <div className="mtl mbs width-30 mobile-width-full">
              <SearchInput placeholder="Search for a country or city"
                value={this.state.searchText}
                onChange={this.updateSearchText} />
            </div>
            <div className="mts mbl width-30 mobile-width-full">
              <Select value={this.state.selectedTimeZone} onChange={this.updateSelectedTimeZone} size="5">
                {this.getFilteredTzInfo().map((tz) => (
                  <option key={tz.name} value={tz.timeZones[0]}>{tz.name}</option>
                ))}
              </Select>
            </div>
            <div className="mvl">
              <DynamicLabelButton
                className="button-primary mrm"
                onClick={this.setTimeZone}
                disabledWhen={this.props.isSaving}
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
