import * as React from 'react';
import {DataRequest} from '../lib/data_request';
import SearchWithResults from '../form/search_with_results';
import autobind from '../lib/autobind';

interface CityInfo {
  name: string
  asciiName: string
  admin?: Option<string>
  country: string
  timeZoneId: string
  population: number
  timeZoneName: string
}

interface CityOption {
  name: string,
  timeZone: string
  timeZoneName: string
  key: string
}

interface Props {
  onChange: (timeZoneId: string, cityName: string, timeZoneName: string) => void,
  defaultTimeZone?: Option<string>
  resetWithNewDefault?: Option<boolean>
}

interface State {
  selectedCity: string,
  selectedOption: Option<CityOption>,
  isSearching: boolean,
  noMatches: boolean,
  cityResults: Array<CityInfo>,
  error: Option<string>
}

class TimeZoneSelector extends React.PureComponent<Props, State> {
    searchInput: Option<SearchWithResults>;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = this.getDefaultState();
    }

    getDefaultState(): State {
      return {
        selectedCity: "",
        selectedOption: null,
        isSearching: false,
        noMatches: false,
        cityResults: [],
        error: null
      };
    }

    getDefaultTimeZone(): string {
      return this.props.defaultTimeZone || this.guessTimeZone();
    }

    componentDidMount(): void {
      this.requestMatchingTimezones(this.getDefaultTimeZone());
    }

    componentWillReceiveProps(nextProps: Props): void {
      if (nextProps.resetWithNewDefault && nextProps.defaultTimeZone !== this.props.defaultTimeZone) {
        this.reset();
      }
    }

    reset(): void {
      if (this.searchInput) {
        this.searchInput.clearSearch();
      }
      this.setDefault();
    }

    setDefault(): void {
      this.setState(this.getDefaultState());
      this.requestMatchingTimezones(this.getDefaultTimeZone());
    }

    guessTimeZone(): string {
      let guessed;
      try {
        guessed = Intl.DateTimeFormat().resolvedOptions().timeZone;
      } catch (e) {
        guessed = 'America/New_York';
      }
      return guessed;
    }

    requestMatchingTimezones(searchQuery: string): void {
      const url = jsRoutes.controllers.ApplicationController.possibleCitiesFor(searchQuery).url;
      this.setState({
        isSearching: true,
        cityResults: [],
        noMatches: false,
        error: null
      }, () => {
        DataRequest
          .jsonGet(url)
          .then((json: {
            matches?: Array<CityInfo>
          }) => {
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

    setSelectedTimeZoneFromCity(cityInfo: CityInfo) {
      const optionProps = this.getOptionPropsFromCityInfo(cityInfo);
      this.updateCityAndOption(optionProps.key, optionProps);
    }

    getOptionPropsFromCityInfo(cityInfo: CityInfo): CityOption {
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

    getFilteredTzInfo(): Array<CityOption> {
      return this.state.cityResults.map(this.getOptionPropsFromCityInfo);
    }

    updateSelectedTimeZone(valueOrNull: Option<string>, newValueIndex: number) {
      const newResult = this.getFilteredTzInfo()[newValueIndex];
      if (newResult) {
        this.updateCityAndOption(newResult.key, newResult);
      }
    }

    updateCityAndOption(cityKey: string, option: CityOption) {
      this.setState({
        selectedCity: cityKey,
        selectedOption: option
      }, () => {
        this.props.onChange(option.timeZone, option.name, option.timeZoneName);
      });
    }

    updateSearchText(newValue: string): void {
      const newQuery = newValue.trim();
      if (newQuery) {
        this.requestMatchingTimezones(newValue);
      } else {
        this.setDefault();
      }
    }

    assembleName(city: string, region: Option<string>, country: string): string {
      return `${city}${region && region !== city ? `, ${region}` : ""}, ${country}`;
    }

    focus(): void {
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

export default TimeZoneSelector;
