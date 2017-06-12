define(function(require) {
  var React = require('react'),
    DataRequest = require('../lib/data_request'),
    Select = require('../form/select'),
    SearchInput = require('../form/search'),
    debounce = require('javascript-debounce');

  return React.createClass({
    displayName: 'TimeZoneSelector',
    propTypes: {
      onChange: React.PropTypes.func.isRequired,
      defaultTimeZone: React.PropTypes.string
    },

    componentDidMount: function() {
      this.delayRequestMatchingTimezones = debounce(this.requestMatchingTimezones, 250);
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
        this.props.onChange(this.state.selectedOption.timeZone, this.state.selectedOption.name);
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

    selectNextMatch: function() {
      this.updateSelectedTimeZone(null, this.refs.results.selectNextItem());
    },

    selectPreviousMatch: function() {
      this.updateSelectedTimeZone(null, this.refs.results.selectPreviousItem());
    },

    assembleName: function(city, region, country) {
      return `${city}${region && region !== city ? `, ${region}` : ""}, ${country}`;
    },

    renderSearchMessage: function() {
      if (this.state.error) {
        return (
          <div className="fade-in maxs pvxs phs type-pink type-bold type-italic">
            {this.state.error}
          </div>
        );
      } else if (this.state.isSearching) {
        return (
          <div className="fade-in maxs pvxs phs type-italic type-disabled">Searching…</div>
        );
      } else if (this.state.noMatches) {
        return (
          <div className="fade-in maxs pvxs phs type-italic type-disabled">No matches found</div>
        );
      }
    },

    render: function() {
      return (
        <div>
          <div className={this.state.isSearching ? "pulse" : ""}>
            <div className="mvl width-30 mobile-width-full">
              <SearchInput placeholder="Search for a city"
                value={this.state.searchText}
                onChange={this.updateSearchText}
                onUpKey={this.selectPreviousMatch}
                onDownKey={this.selectNextMatch}
                withResults={true}
              />
              <div className="position-relative">
                <Select
                  ref="results"
                  value={this.state.selectedCity}
                  onChange={this.updateSelectedTimeZone}
                  size="5"
                  withSearch={true}
                >
                  {this.getFilteredTzInfo().map((tz) => (
                    <option key={tz.key} value={tz.key}>{tz.name}</option>
                  ))}
                </Select>
                <div className="position-absolute position-z-popup position-top-left">
                  {this.renderSearchMessage()}
                </div>
              </div>
            </div>
          </div>
          <div className="mvl">
            <span className="mrm">Selected time zone:</span>
            <b>{this.state.selectedOption ? this.state.selectedOption.name : (
              <i className="type-disabled">None</i>
            )}</b>
          </div>
        </div>
      );
    }
  });
});
