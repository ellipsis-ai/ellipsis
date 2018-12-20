import * as React from 'react';
import autobind from '../lib/autobind';
import BehaviorGroup from "../models/behavior_group";
import BehaviorVersion from "../models/behavior_version";
import Editable from "../models/editable";
import Button from "../form/button";
import SearchWithResults, {SearchOption} from "../form/search_with_results";
import Sort from "../lib/sort";

interface Props {
  group: BehaviorGroup
  onSelect: (groupId: Option<string>, selectedId: Option<string>) => void
  onDone: () => void
}

interface State {
  results: Array<{
    name: string,
    value: string
  }>
  currentResultValue: string,
  hasMatches: boolean
}

interface RankedSearchOption extends SearchOption {
  rank: number
}

class QuickSearchPanel extends React.Component<Props, State> {
  searchInput: Option<SearchWithResults>;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      results: [],
      currentResultValue: "",
      hasMatches: false
    };
  }

  onChangeSearchText(newValue: string): void {
    const newResults = this.getResultsFor(newValue);
    const firstResult = newResults[0] ? newResults[0].value : null;
    this.setState({
      results: newResults,
      currentResultValue: firstResult || "",
      hasMatches: Boolean(firstResult)
    });
  }

  onSelect(): void {
    if (this.state.currentResultValue) {
      this.props.onSelect(this.props.group.id, this.state.currentResultValue);
      this.onDone();
    }
  }

  onEscKey(): void {
    this.onDone();
  }

  onDone(): void {
    this.props.onDone();
    if (this.searchInput) {
      this.searchInput.clearSearch()
    }
  }

  onSelectResult(newValue: string): void {
    this.setState({
      currentResultValue: newValue
    });
  }

  tokenize(text: string): Array<string> {
    return text.trim().toLowerCase().replace(/\s+/g, " ").split(" ");
  }

  anyTokenMatches(text: string, searchText: string): boolean {
    return this.tokenize(text).some((ea) => ea.startsWith(searchText));
  }

  actionMatchesSearch(action: BehaviorVersion, searchText: string): boolean {
    return this.editableMatchesSearch(action, searchText) ||
      action.triggers.some((trigger) => this.anyTokenMatches(trigger.getText(), searchText));
  }

  editableMatchesSearch(editable: Editable, searchText: string): boolean {
    return this.anyTokenMatches(editable.getName(), searchText);
  }

  rankFor(text: string, searchText: string): number {
    const index = text.split(" ").findIndex((token) => token.toLowerCase().startsWith(searchText));
    return index >= 0 ? index : Infinity;
  }

  getResultsFor(rawSearchText: string): Array<RankedSearchOption> {
    const searchText = rawSearchText.trim().toLowerCase();
    if (!searchText) {
      return [];
    }

    const actionOptions = this.props.group.getActions()
      .filter((action) => this.actionMatchesSearch(action, searchText))
      .map((action) => {
        const triggers = action.triggers.filter((trigger) => !trigger.isRegex).map((trigger) => trigger.displayText());
        const forRanking = `${action.getName()} ${triggers.join(" ")}`;
        const name = `${action.icon()} ${action.getName()}${triggers.length > 0 ? " · Triggers: " + triggers.join(" · ") : ""}`;
        return {
          name: name,
          value: action.behaviorId,
          rank: this.rankFor(forRanking, searchText)
        };
      });

    const dataTypeOptions = this.props.group.getDataTypes()
      .filter((dataType) => this.editableMatchesSearch(dataType, searchText))
      .map((dataType) => {
        return {
          name: `${dataType.icon()} ${dataType.getName()}`,
          value: dataType.behaviorId,
          rank: this.rankFor(dataType.getName(), searchText)
        };
      });

    const libraryOptions = this.props.group.getLibraries()
      .filter((library) => this.editableMatchesSearch(library, searchText))
      .map((library) => {
        return {
          name: `${library.icon()} ${library.getName()}`,
          value: library.libraryId,
          rank: this.rankFor(library.getName(), searchText)
        };
      });

    const testOptions = this.props.group.getTests()
      .filter((test) => this.editableMatchesSearch(test, searchText))
      .map((test) => {
        return {
          name: `${test.icon()} ${test.getName()}`,
          value: test.behaviorId,
          rank: this.rankFor(test.getName(), searchText)
        };
      });

    return Sort.arrayAlphabeticalBy(actionOptions.concat(dataTypeOptions, libraryOptions, testOptions), (ea) => `${ea.rank}${ea.name}`);
  };

  render() {
    return (
      <div className="box-action phn">
        <div className="container container-c">
          <h4 className="mvn">Switch to any action, data type, library or test in this skill</h4>

          <SearchWithResults
            ref={(el) => this.searchInput = el}
            placeholder={"Search by name"}
            value={this.state.currentResultValue}
            options={this.state.results}
            isSearching={false}
            noMatches={!this.state.hasMatches}
            onChangeSearch={this.onChangeSearchText}
            onSelect={this.onSelectResult}
            onEnterKey={this.onSelect}
            onEscKey={this.onEscKey}
          />

          <Button className="button-primary mrs" onClick={this.onSelect} disabled={!this.state.currentResultValue}>Switch to selection</Button>
          <Button onClick={this.onDone}>Cancel</Button>

        </div>
      </div>
    );
  }
}

export default QuickSearchPanel;
