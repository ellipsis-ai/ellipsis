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
      this.setDefaultTimeZone();
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
        selectedCity: "",
        selectedTimeZone: "",
        selectedTimeZoneName: "",
        defaultTimeZone: "",
        defaultTimeZoneName: "",
        searchText: "",
        isSearching: false,
        noMatches: false,
        cityResults: []
      };
    },

    requestMatchingTimezones: function(searchQuery, optionalCallback) {
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
                if (matches.length > 0) {
                  this.setSelectedTimeZoneFromCity(matches[0], optionalCallback);
                } else {
                  this.resetToDefaultTimeZone();
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

    setSelectedTimeZoneFromCity: function(cityInfo, optionalCallback) {
      const optionProps = this.getOptionPropsFromCityInfo(cityInfo);
      this.setState({
        selectedCity: optionProps.key,
        selectedTimeZone: cityInfo.timeZoneId,
        selectedTimeZoneName: optionProps.name
      }, optionalCallback);
    },

    setDefaultTimeZone: function() {
      this.requestMatchingTimezones(this.state.guessedTimeZone, () => {
        if (this.state.selectedTimeZone) {
          this.setState({
            selectedCity: "",
            defaultTimeZone: this.state.selectedTimeZone,
            defaultTimeZoneName: this.state.selectedTimeZoneName
          });
        }
      });
    },

    resetToDefaultTimeZone: function() {
      if (this.state.defaultTimeZone) {
        this.setState({
          selectedCity: "",
          selectedTimeZone: this.state.defaultTimeZone,
          selectedTimeZoneName: this.state.defaultTimeZoneName
        });
      }
    },

    getOptionPropsFromCityInfo: function(cityInfo) {
      const name = this.assembleName(cityInfo.name, cityInfo.admin, cityInfo.country);
      const timeZone = cityInfo.timeZoneId;
      const key = [name, timeZone].join("|");
      return {
        name: name,
        timeZone: timeZone,
        key: key
      };
    },

    getFilteredTzInfo: function() {
      return this.state.cityResults.map(this.getOptionPropsFromCityInfo);
    },

    updateSelectedTimeZone: function(newValue, newValueIndex) {
      this.setState({
        selectedCity: newValue,
        selectedTimeZone: this.getFilteredTzInfo()[newValueIndex].timeZone,
        selectedTimeZoneName: this.getFilteredTzInfo()[newValueIndex].name
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

    getDisplayNameFor: function(tzId) {
      var targetTz = timeZoneList.find((ea) => ea.timeZones.includes(tzId));
      if (targetTz) {
        return targetTz.name;
      } else {
        return this.state.selectedTimeZone.replace(/_/g, ' ');
      }
    },

    assembleName: function(city, region, country) {
      return `${city}${region && region !== city ? `, ${region}` : ""}, ${country}`;
    },

    setTimeZone: function() {
      this.props.onSetTimeZone(this.state.selectedTimeZone, this.state.selectedTimeZoneName);
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

            <div className={this.state.isSearching ? "pulse" : ""}>
              <div className="mtl mbs width-30 mobile-width-full">
                <SearchInput placeholder="Search for a city"
                  value={this.state.searchText}
                  onChange={this.updateSearchText}/>
              </div>
              <div className="mts mbl width-30 mobile-width-full">
                <Select value={this.state.selectedCity} onChange={this.updateSelectedTimeZone} size="5">
                  {this.getFilteredTzInfo().map((tz) => (
                    <option key={tz.key} value={tz.key}>{tz.name}</option>
                  ))}
                </Select>
              </div>
            </div>
            <div className="mvl">
              <span className="mrm">Selected time zone:</span>
              <b>{this.state.selectedTimeZoneName}</b>
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
