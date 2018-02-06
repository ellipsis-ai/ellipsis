import * as React from 'react';
import DataRequest from '../../javascripts/lib/data_request';
import SearchWithResults from '../../javascripts/form/search_with_results';
import autobind from '../../javascripts/lib/autobind';

class TimeZoneSelector extends React.PureComponent {
    constructor(props) {
      super(props);
      autobind(this);
      this.state = this.getDefaultState();
    }

    getDefaultState() {
      return {
        selectedCity: "",
        selectedOption: null,
        isSearching: false,
        noMatches: false,
        cityResults: [],
        error: null
      };
    }

    getDefaultTimeZone() {
      return this.props.defaultTimeZone || this.guessTimeZone();
    }

    componentDidMount() {
      this.requestMatchingTimezones(this.getDefaultTimeZone());
    }

    componentWillReceiveProps(nextProps) {
      if (nextProps.resetWithNewDefault && nextProps.defaultTimeZone !== this.props.defaultTimeZone) {
        this.reset();
      }
    }

    reset() {
      this.searchInput.clearSearch();
      this.setDefault();
    }

    setDefault() {
      this.setState(this.getDefaultState());
      this.requestMatchingTimezones(this.getDefaultTimeZone());
    }

    guessTimeZone() {
      let guessed;
      try {
        guessed = Intl.DateTimeFormat().resolvedOptions().timeZone;
      } catch (e) {
        guessed = 'America/New_York';
      }
      return guessed;
    }

    requestMatchingTimezones(searchQuery) {
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
    }

    setSelectedTimeZoneFromCity(cityInfo) {
      const optionProps = this.getOptionPropsFromCityInfo(cityInfo);
      this.updateCityAndOption(optionProps.key, optionProps);
    }

    getOptionPropsFromCityInfo(cityInfo) {
      const name = this.assembleName(cityInfo.name, cityInfo.admin, cityInfo.country);
      const timeZone = cityInfo.timeZoneId;
      const key = [name, timeZone].join("|");
      return {
        name: name,
        timeZone: timeZone,
        timeZoneName: cityInfo.timeZoneName,
        key: key
      };
    }

    getFilteredTzInfo() {
      return this.state.cityResults.map(this.getOptionPropsFromCityInfo);
    }

    updateSelectedTimeZone(valueOrNull, newValueIndex) {
      var newResult = this.getFilteredTzInfo()[newValueIndex];
      if (newResult) {
        this.updateCityAndOption(newResult.key, newResult);
      }
    }

    updateCityAndOption(cityKey, option) {
      this.setState({
        selectedCity: cityKey,
        selectedOption: option
      }, () => {
        this.props.onChange(option.timeZone, option.name, option.timeZoneName);
      });
    }

    updateSearchText(newValue) {
      const newQuery = newValue.trim();
      if (newQuery) {
        this.requestMatchingTimezones(newValue);
      } else {
        this.setDefault();
      }
    }

    assembleName(city, region, country) {
      return `${city}${region && region !== city ? `, ${region}` : ""}, ${country}`;
    }

    focus() {
      if (this.searchInput) {
        this.searchInput.focus();
      }
    }

    render() {
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
            <span className="mrm">Time zone:</span>
            {this.state.selectedOption ? (
              <span>
                <b>{this.state.selectedOption.timeZoneName} </b>
                <span>({this.state.selectedOption.name})</span>
              </span>
            ) : (
              <i className="type-disabled">None</i>
            )}
          </div>
        </div>
      );
    }
}

TimeZoneSelector.propTypes = {
  onChange: React.PropTypes.func.isRequired,
  defaultTimeZone: React.PropTypes.string,
  resetWithNewDefault: React.PropTypes.bool
};

export default TimeZoneSelector;
