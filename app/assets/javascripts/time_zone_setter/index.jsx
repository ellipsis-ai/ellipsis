define(function(require) {
  var React = require('react'),
    DataRequest = require('../lib/data_request'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    Select = require('../form/select'),
    SearchInput = require('../form/search'),
    timeZoneList = require('./tz_info'),
    debounce = require('javascript-debounce');

  return React.createClass({
    displayName: 'TimeZoneSetter',
    propTypes: {
      onSetTimeZone: React.PropTypes.func.isRequired,
      isSaving: React.PropTypes.bool,
      error: React.PropTypes.string
    },

    componentDidMount: function() {
      this.delayRequestMatchingTimezones = debounce(this.requestMatchingTimezones, 250);
    },

    guessTimeZone: function() {
      let guessed;
      try {
        guessed = Intl.DateTimeFormat().resolvedOptions().timeZone;
      } catch (e) {
        guessed = 'America/New_York';
      }
      return guessed;
    },

    getInitialState: function() {
      var guessedTimeZone = this.guessTimeZone();
      return {
        guessedTimeZone: guessedTimeZone,
        selectedTimeZone: guessedTimeZone,
        searchText: "",
        isSearching: false,
        noMatches: false,
        cityResults: []
      };
    },

    requestMatchingTimezones: function(searchQuery) {
      const url = jsRoutes.controllers.ApplicationController.possibleCitiesFor(searchQuery).url;
      this.setState({
        isSearching: true,
        noMatches: false
      }, () => {
        DataRequest
          .jsonGet(url)
          .then((json) => {
            const matches = json.matches;
            if (matches) {
              this.setState({
                isSearching: false,
                noMatches: matches.length === 0,
                cityResults: matches
              }, () => {
                if (!matches.some((city) => city.timeZoneId === this.state.selectedTimeZone)) {
                  this.setSelectedTimeZoneMatching(matches[0].timeZoneId);
                }
              });
            } else {
              throw new Error("Error loading search results");
            }
          })
          .catch(() => {
            this.setState({
              isSearching: false,
              noMatches: false,
              cityResults: []
            });
          });
      });
    },

    setSelectedTimeZoneMatching: function(tzId) {
      const matched = timeZoneList.find((tz) => tz.timeZones.includes(tzId));
      this.setState({
        selectedTimeZone: this.state.noMatches ? this.state.guessedTimeZone : matched.timeZones[0]
      });
    },

    getFilteredTzInfo: function() {
      var searchText = (this.state.searchText || "").trim().toLowerCase();
      if (searchText) {
        if (this.state.noMatches) {
          return [];
        } else {
          return timeZoneList.filter((tzInfo) => {
            return this.state.cityResults.some((cityInfo) => tzInfo.timeZones.includes(cityInfo.timeZoneId)) ||
              tzInfo.name.toLowerCase().includes(searchText) ||
              tzInfo.timeZones.some((tzId) => tzId.toLowerCase().includes(searchText));
          }).map((tzInfo) => {
            const cityMatch = this.state.cityResults.find((city) => tzInfo.timeZones.some((tzId) => tzId === city.timeZoneId));
            const includeMatchedCity = cityMatch && tzInfo.name.indexOf(cityMatch.name) !== 0;
            return {
              name: includeMatchedCity ? `${cityMatch.name} (${tzInfo.name})` : tzInfo.name,
              timeZones: tzInfo.timeZones
            };
          });
        }
      } else {
        return timeZoneList;
      }
    },

    updateSelectedTimeZone: function(newValue) {
      this.setState({
        selectedTimeZone: newValue
      });
    },

    updateSearchText: function(newValue) {
      const newQuery = newValue.trim();
      if (newQuery) {
        this.setState({
          searchText: newValue
        }, () => {
          this.delayRequestMatchingTimezones(newValue);
        });
      } else {
        this.setState({
          searchText: newValue,
          noMatches: false,
          cityResults: []
        });
      }
    },

    getCurrentDisplayName: function() {
      var targetTz = timeZoneList.find((ea) => ea.timeZones.includes(this.state.selectedTimeZone));
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
              This will be used when Ellipsis displays dates and times to a group, or whenever a time of day mentioned
              isn’t otherwise obvious.
            </p>

            <div className="mvl">
              <span className="align-button mrm">Selected time zone:</span>
              <Select className="width-15" value={this.state.selectedTimeZone} onChange={this.updateSelectedTimeZone}>
                {timeZoneList.map((tz) => (
                  <option key={tz.name} value={tz.timeZones[0]}>{tz.name}</option>
                ))}
              </Select>
            </div>
            <div className="mtl mbs width-30 mobile-width-full">
              <SearchInput placeholder="Search for a country or city"
                value={this.state.searchText}
                onChange={this.updateSearchText}/>
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
