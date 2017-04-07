define(function(require) {
  var React = require('react'),
    DataRequest = require('../lib/data_request'),
    DynamicLabelButton = require('../form/dynamic_label_button'),
    Select = require('../form/select'),
    SearchInput = require('../form/search'),
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
      this.requestMatchingTimezones(this.state.guessedTimeZone);
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
        selectedOption: null,
        searchText: "",
        isSearching: false,
        noMatches: false,
        cityResults: [],
        error: null
      };
    },

    requestMatchingTimezones: function(searchQuery) {
      const url = jsRoutes.controllers.ApplicationController.possibleCitiesFor(searchQuery).url;
      this.setState({
        isSearching: true,
        noMatches: false,
        error: null
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
                  this.setSelectedTimeZoneFromCity(matches[0]);
                }
              });
            } else {
              throw new Error();
            }
          })
          .catch(() => {
            this.setState({
              isSearching: false,
              noMatches: false,
              cityResults: [],
              error: "An error occurred while loading search results."
            });
          });
      });
    },

    setSelectedTimeZoneFromCity: function(cityInfo) {
      const optionProps = this.getOptionPropsFromCityInfo(cityInfo);
      this.setState({
        selectedCity: optionProps.key,
        selectedOption: optionProps
      });
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
        selectedOption: this.getFilteredTzInfo()[newValueIndex]
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

    assembleName: function(city, region, country) {
      return `${city}${region && region !== city ? `, ${region}` : ""}, ${country}`;
    },

    setTimeZone: function() {
      this.props.onSetTimeZone(this.state.selectedOption.timeZone, this.state.selectedOption.name);
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
              <b>{this.state.selectedOption ? this.state.selectedOption.name : (
                <i className="type-disabled">None</i>
              )}</b>
            </div>
            <div className="mvl">
              <DynamicLabelButton
                className="button-primary mrm"
                onClick={this.setTimeZone}
                disabledWhen={this.props.isSaving || !this.state.selectedOption}
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
            <div className="mvl">
              {this.state.error ? (
                <div className="fade-in type-pink type-bold type-italic">
                  {this.state.error}
                </div>
              ) : null}
            </div>
          </div>
        </div>
      );
    }
  });
});
