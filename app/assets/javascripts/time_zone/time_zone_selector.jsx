define(function(require) {
  var React = require('react'),
    DataRequest = require('../lib/data_request'),
    SearchWithResults = require('../form/search_with_results');

  return React.createClass({
    displayName: 'TimeZoneSelector',
    propTypes: {
      onChange: React.PropTypes.func.isRequired,
      defaultTimeZone: React.PropTypes.string
    },

    componentDidMount: function() {
      this.requestMatchingTimezones(this.state.defaultTimeZone);
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
      var defaultTimeZone = this.props.defaultTimeZone || this.guessTimeZone();
      return {
        defaultTimeZone: defaultTimeZone,
        selectedCity: "",
        selectedOption: null,
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
        cityResults: [],
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
      this.updateCityAndOption(optionProps.key, optionProps);
    },

    getOptionPropsFromCityInfo: function(cityInfo) {
      const name = this.assembleName(cityInfo.name, cityInfo.admin, cityInfo.country);
      const timeZone = cityInfo.timeZoneId;
      const key = [name, timeZone].join("|");
      return {
        name: name,
        timeZone: timeZone,
        timeZoneName: cityInfo.timeZoneName,
        key: key
      };
    },

    getFilteredTzInfo: function() {
      return this.state.cityResults.map(this.getOptionPropsFromCityInfo);
    },

    updateSelectedTimeZone: function(valueOrNull, newValueIndex) {
      var newResult = this.getFilteredTzInfo()[newValueIndex];
      if (newResult) {
        this.updateCityAndOption(newResult.key, newResult);
      }
    },

    updateCityAndOption: function(cityKey, option) {
      this.setState({
        selectedCity: cityKey,
        selectedOption: option
      }, () => {
        this.props.onChange(option.timeZone, option.name, option.timeZoneName);
      });
    },

    updateSearchText: function(newValue) {
      const newQuery = newValue.trim();
      if (newQuery) {
        this.requestMatchingTimezones(newValue);
      } else {
        this.setState({
          noMatches: false,
          cityResults: []
        });
      }
    },

    assembleName: function(city, region, country) {
      return `${city}${region && region !== city ? `, ${region}` : ""}, ${country}`;
    },

    focus: function() {
      if (this.searchInput) {
        this.searchInput.focus();
      }
    },

    render: function() {
      return (
        <div>
          <SearchWithResults
            ref={(searchInput) => this.searchInput = searchInput}
            placeholder="Search for a city"
            value={this.state.selectedCity}
            options={this.getFilteredTzInfo().map((tz) => ({ name: tz.name, value: tz.key }))}
            isSearching={this.state.isSearching}
            noMatches={this.state.noMatches}
            error={this.state.error}
            onChangeSearch={this.updateSearchText}
            onSelect={this.updateSelectedTimeZone}
          />
          <div className="mvl">
            <span className="mrm">Selected time zone:</span>
            <b>{this.state.selectedOption ? (
              <span>
                <span>{this.state.selectedOption.name}</span>
                <span> ({this.state.selectedOption.timeZoneName})</span>
              </span>
            ) : (
              <i className="type-disabled">None</i>
            )}</b>
          </div>
        </div>
      );
    }
  });
});
