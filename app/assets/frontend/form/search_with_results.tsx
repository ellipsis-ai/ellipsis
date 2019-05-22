import * as React from 'react';
import SearchInput from './search';
import Select from './select';
import * as debounce from 'javascript-debounce';
import autobind from "../lib/autobind";

export interface SearchOption {
  name: string
  value: string
}

interface Props {
  placeholder?: string
  value: string,
  options: Array<SearchOption>,
  isSearching: boolean,
  noMatches: boolean,
  error?: Option<string>
  onChangeSearch: (newSearch: string) => void,
  onSelect: (newValue: string, newIndex: number) => void,
  onEnterKey?: (value: string) => void
  onEscKey?: () => void
  fullWidth?: boolean
}

interface State {
  searchText: string
}

class SearchWithResults extends React.Component<Props, State> {
    input: Option<SearchInput>;
    selector: Option<Select>;
    delayChangeSearch: (newSearch: string) => void;

    constructor(props: Props) {
      super(props);
      autobind(this);
      this.delayChangeSearch = debounce(this.onChangeSearch, 100);
      this.state = {
        searchText: ""
      };
    }

    componentDidMount(): void {
      if (this.selector) {
        this.selector.scrollToSelectedOption();
      }
    }

    componentDidUpdate(prevProps: Props): void {
      if (prevProps.options !== this.props.options && this.selector) {
        this.selector.scrollToSelectedOption();
      }
    }

    onChangeSearch(newSearch: string): void {
      this.props.onChangeSearch(newSearch);
    }

    onSelect(newValue: string, newIndex: number): void {
      this.props.onSelect(newValue, newIndex);
    }

    onSelectNext(): void {
      if (this.selector) {
        this.selector.selectNextItem();
        const newValue = this.selector.getCurrentValue();
        const newIndex = this.selector.getCurrentIndex();
        if (typeof newValue === "string" && typeof newIndex  === 'number') {
          this.onSelect(newValue, newIndex);
        }
      }
    }

    onSelectPrevious(): void {
      if (this.selector) {
        this.selector.selectPreviousItem();
        const newValue = this.selector.getCurrentValue();
        const newIndex = this.selector.getCurrentIndex();
        if (typeof newValue === "string" && typeof newIndex === 'number') {
          this.onSelect(newValue, newIndex);
        }
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

    focus(): void {
      if (this.input) {
        this.input.focus();
      }
    }

    onEnterKey(): void {
      if (this.props.onEnterKey) {
        this.props.onEnterKey(this.props.value);
      }
    }

    clearSearch(): void {
      if (this.input) {
        this.input.clearValue();
      }
    }

    render() {
      return (
        <div>
          <div className={this.props.isSearching ? "pulse" : ""}>
            <div className={`mvl ${this.props.fullWidth ? "width-full" : "width-30 mobile-width-full"}`}>
              <SearchInput
                ref={(input) => this.input = input}
                placeholder={this.props.placeholder}
                value={this.state.searchText}
                onChange={this.updateSearchText}
                onUpKey={this.onSelectPrevious}
                onDownKey={this.onSelectNext}
                onEnterKey={this.onEnterKey}
                onEscKey={this.props.onEscKey}
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
