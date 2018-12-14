import * as React from 'react';
import SearchInput from './search';
import Select from './select';
import * as debounce from 'javascript-debounce';
import autobind from "../lib/autobind";

export interface LabeledOptionGroup {
  label: string
  options: Array<{
    name: string,
    value: string
  }>
}

interface Props {
  placeholder?: string
  value: string
  optionGroups: Array<LabeledOptionGroup>,
  isSearching: boolean
  noMatches: boolean
  error?: Option<string>
  onChangeSearch: (newSearchText: string) => void
  onSelect: (newValue: string, newIndex: number) => void,
  onEnterKey?: Option<(value: string) => void> 
}

interface State {
  searchText: string
}

class SearchWithGroupedResults extends React.Component<Props, State> {
  delayChangeSearch: (newSearchText: string) => void;
  selector: Option<Select>;
  input: Option<SearchInput>;

  constructor(props: Props) {
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

  componentDidUpdate(prevProps: Props) {
    if (prevProps.optionGroups !== this.props.optionGroups && this.selector) {
      this.selector.scrollToSelectedOption();
    }
  }

  onChangeSearch(newSearch: string): void {
    this.props.onChangeSearch(newSearch);
  }

  onSelect(newValue: Option<string>, newIndex: Option<number>): void {
    if (typeof newValue === "string" && typeof newIndex === "number") {
      this.props.onSelect(newValue, newIndex);
    }
  }

  onSelectNext() {
    if (this.selector) {
      this.selector.selectNextItem();
      this.onSelect(this.selector.getCurrentValue(), this.selector.getCurrentIndex());
    }
  }

  onSelectPrevious(): void {
    if (this.selector) {
      this.selector.selectPreviousItem();
      this.onSelect(this.selector.getCurrentValue(), this.selector.getCurrentIndex());
    }
  }

  updateSearchText(newValue: string): void {
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

  renderGroup(group: LabeledOptionGroup) {
    if (group.options.length > 0) {
      return (
        <optgroup label={group.label} key={group.label || "empty"}>
          {group.options.map(ea => {
            return (
              <option key={ea.value} value={ea.value} className={ea.value ? "" : "type-italic"}>{ea.name}</option>
            );
          })}
        </optgroup>
      );
    } else {
      return null;
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
                {this.props.optionGroups.map(this.renderGroup)}
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

export default SearchWithGroupedResults;
