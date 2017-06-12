define(function(require) {
  var React = require('react'),
    SearchInput = require('./search'),
    Select = require('./select'),
    debounce = require('javascript-debounce');

  return React.createClass({
    displayName: 'SearchWithResults',
    propTypes: {
      placeholder: React.PropTypes.string,
      value: React.PropTypes.string.isRequired,
      options: React.PropTypes.arrayOf(React.PropTypes.shape({
        name: React.PropTypes.string.isRequired,
        value: React.PropTypes.string.isRequired
      })).isRequired,
      isSearching: React.PropTypes.bool.isRequired,
      noMatches: React.PropTypes.bool.isRequired,
      error: React.PropTypes.string,
      onChangeSearch: React.PropTypes.func.isRequired,
      onSelect: React.PropTypes.func.isRequired,
      onEnterKey: React.PropTypes.func
    },

    getInitialState: function() {
      return {
        searchText: ""
      };
    },

    componentDidMount: function() {
      this.delayChangeSearch = debounce(this.onChangeSearch, 250);
    },

    onChangeSearch: function(newSearch) {
      this.props.onChangeSearch(newSearch);
    },

    onSelect: function(newValue) {
      this.props.onSelect(newValue);
    },

    onSelectNext: function() {
      if (this.selector) {
        this.selector.selectNextItem();
        this.onSelect(this.selector.getCurrentValue());
      }
    },

    onSelectPrevious: function() {
      if (this.selector) {
        this.selector.selectPreviousItem();
        this.onSelect(this.selector.getCurrentValue());
      }
    },

    updateSearchText: function(newValue) {
      const newQuery = newValue.trim();
      if (newQuery) {
        this.setState({
          searchText: newValue
        }, () => {
          this.delayChangeSearch(newValue);
        });
      } else {
        this.setState({
          searchText: newValue
        }, () => {
          this.onChangeSearch(newQuery);
        });
      }
    },

    renderSearchMessage: function() {
      if (this.props.error) {
        return (
          <div className="fade-in maxs pvxs phs type-pink type-bold type-italic">
            {this.props.error}
          </div>
        );
      } else if (this.props.isSearching) {
        return (
          <div className="fade-in maxs pvxs phs type-italic type-disabled">Searching…</div>
        );
      } else if (this.props.noMatches) {
        return (
          <div className="fade-in maxs pvxs phs type-italic type-disabled">No matches found</div>
        );
      }
    },

    focus: function() {
      if (this.input) {
        this.input.focus();
      }
    },

    onEnterKey: function() {
      if (this.props.onEnterKey) {
        this.props.onEnterKey(this.props.value);
      }
    },

    clearSearch: function() {
      if (this.input) {
        this.input.clearValue();
      }
    },

    render: function() {
      return (
        <div>
          <div className={this.state.isSearching ? "pulse" : ""}>
            <div className="mvl width-30 mobile-width-full">
              <SearchInput
                ref={(input) => this.input = input}
                placeholder={this.props.placeholder}
                value={this.state.searchText}
                onChange={this.updateSearchText}
                onUpKey={this.onSelectPrevious}
                onDownKey={this.onSelectNext}
                onEnterKey={this.onEnterKey}
                withResults={true}
              />
              <div className="position-relative">
                <Select
                  ref={(selector) => this.selector = selector}
                  value={this.props.value}
                  onChange={this.onSelect}
                  size="5"
                  withSearch={true}
                >
                  {this.props.options.map((ea) => (
                    <option key={ea.value} value={ea.value}>{ea.name}</option>
                  ))}
                </Select>
                <div className="position-absolute position-z-popup position-top-left">
                  {this.renderSearchMessage()}
                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
  });
});
