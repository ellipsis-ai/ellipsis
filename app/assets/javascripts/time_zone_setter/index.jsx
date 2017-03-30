define(function(require) {
  var React = require('react'),
    Select = require('../form/select'),
    SearchInput = require('../form/search'),
    tzInfo = require('./tz_info');

  return React.createClass({
    displayName: 'TimeZoneSetter',
    propTypes: {
      // string: React.PropTypes.string.isRequired,
      // callback: React.PropType.func.isRequired,
      // children: React.PropTypes.node.isRequired
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

    render: function() {
      return (
        <div className="bg-white border-bottom border-bottom-thick pvxl">
          <div className="container container-c container-narrow">
            <h3>Set your team’s time zone</h3>
            <p>
              This will be used as the default when Ellipsis displays dates and times, or when handling schedules.
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
              <SearchInput placeholder="Search for a country or city" value={this.state.searchText}
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
              <button type="button" className="button-primary">Set team time zone</button>
            </div>
          </div>
        </div>
      );
    }
  });
});
