import * as React from 'react';
import SearchInput from './search';
import Select from './select';
import * as debounce from 'javascript-debounce';
import autobind from "../lib/autobind";

interface Props {
  placeholder?: string
  value: string,
  options: Array<{
    name: string,
    value: string
  }>,
  isSearching: boolean,
  noMatches: boolean,
  error?: Option<string>
  onChangeSearch: (newSearch: string) => void,
  onSelect: (newValue: string, newIndex: number) => void,
  onEnterKey?: (value: string) => void
}

interface State {
  searchText: string
}

class SearchWithResults extends React.Component<Props, State> {
    input: Option<SearchInput>;
    selector: Option<Select>;
    delayChangeSearch: (newSearch: string) => void;

    constructor(props) {
      super(props);
      autobind(this);
      this.delayChangeSearch = debounce(this.onChangeSearch, 250);
      this.state = {
        searchText: ""
      };
    }

    componentDidMount() {
      if (this.selector) {
        this.selector.scrollToSelectedOption();
      }
    }

    componentDidUpdate(prevProps) {
      if (prevProps.options !== this.props.options && this.selector) {
        this.selector.scrollToSelectedOption();
      }
    }

    onChangeSearch(newSearch) {
      this.props.onChangeSearch(newSearch);
    }

    onSelect(newValue, newIndex) {
      this.props.onSelect(newValue, newIndex);
    }

    onSelectNext() {
      if (this.selector) {
        this.selector.selectNextItem();
        this.onSelect(this.selector.getCurrentValue(), this.selector.getCurrentIndex());
      }
    }

    onSelectPrevious() {
      if (this.selector) {
        this.selector.selectPreviousItem();
        this.onSelect(this.selector.getCurrentValue(), this.selector.getCurrentIndex());
      }
    }

    updateSearchText(newValue) {
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
    }

    renderSearchMessage() {
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
      } else {
        return null;
      }
    }

    focus() {
      if (this.input) {
        this.input.focus();
      }
    }

    onEnterKey() {
      if (this.props.onEnterKey) {
        this.props.onEnterKey(this.props.value);
      }
    }

    clearSearch() {
      if (this.input) {
        this.input.clearValue();
      }
    }

    render() {
      return (
        <div>
          <div className={this.props.isSearching ? "pulse" : ""}>
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
                  size={5}
                  withSearch={true}
                >
                  {this.props.options.map((ea) => (
                    <option key={ea.value} value={ea.value} className={ea.value ? "" : "type-italic"}>{ea.name}</option>
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
}

export default SearchWithResults;
