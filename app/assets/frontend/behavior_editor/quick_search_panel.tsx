import * as React from 'react';
import autobind from '../lib/autobind';
import BehaviorGroup from "../models/behavior_group";
import BehaviorVersion from "../models/behavior_version";
import Editable from "../models/editable";
import Button from "../form/button";
import SearchWithResults, {SearchOption} from "../form/search_with_results";
import Sort from "../lib/sort";
import Textarea from "../form/textarea";

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
  currentSearchValue: string,
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
      currentSearchValue: "",
      hasMatches: false
    };
  }

  onChangeSearchText(newValue: string): void {
    const newResults = this.getResultsFor(newValue);
    const firstResult = newResults[0] ? newResults[0].value : null;
    this.setState({
      results: newResults,
      currentResultValue: firstResult || "",
      currentSearchValue: newValue,
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

  tokenize(text: string, forCode: boolean): Array<string> {
    const searchPattern = forCode ? /\W+/g : /\s+/g;
    return text.trim().toLowerCase().replace(searchPattern, " ").split(" ");
  }

  anyTokenMatches(text: string, searchText: string, forCode: boolean): boolean {
    return this.tokenize(text, forCode).some((ea) => ea.startsWith(searchText));
  }

  actionMatchesSearch(action: BehaviorVersion, searchText: string): boolean {
    return this.editableMatchesSearch(action, searchText) ||
      action.triggers.some((trigger) => this.anyTokenMatches(trigger.getText(), searchText, false)) ||
      this.anyTokenMatches(action.functionBody, searchText, false) ||
      this.anyTokenMatches(action.functionBody, searchText, true);
  }

  editableMatchesSearch(editable: Editable, searchText: string): boolean {
    return this.anyTokenMatches(editable.getName(), searchText, false) ||
      this.anyTokenMatches(editable.functionBody, searchText, false) ||
      this.anyTokenMatches(editable.functionBody, searchText, true);
  }

  rankFor(text: string, searchText: string): number {
    const index = text.split(" ").findIndex((token) => token.toLowerCase().startsWith(searchText));
    return index >= 0 ? index : Infinity;
  }

  editableToOption(editable: Editable, searchText: string): RankedSearchOption {
    return {
      name: `${editable.icon()} ${editable.getName()}`,
      value: editable.getPersistentId(),
      rank: this.rankFor(editable.getName(), searchText)
    };
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
          value: action.getPersistentId(),
          rank: this.rankFor(forRanking, searchText)
        };
      });

    const otherEditables = ([] as Array<Editable>).concat(this.props.group.getDataTypes(), this.props.group.getLibraries(), this.props.group.getTests());
    const otherOptions = otherEditables.filter((ea) => this.editableMatchesSearch(ea, searchText))
      .map((ea) => this.editableToOption(ea, searchText));

    return Sort.arrayAlphabeticalBy(actionOptions.concat(otherOptions), (ea) => `${ea.rank}${ea.name}`);
  };

  getCurrentSelectionFunctionLines(): {
    start: number,
    lines: Array<string>
  } {
    const id = this.state.currentResultValue;
    const editable = this.props.group.behaviorVersions.find((ea) => ea.getPersistentId() === id) ||
      this.props.group.libraryVersions.find((ea) => ea.getPersistentId() === id);
    if (editable) {
      const functionLines = editable.functionBody.split("\n");
      const firstRelevantLine = functionLines.findIndex((line) => line.toLowerCase().includes(this.state.currentSearchValue));
      if (firstRelevantLine !== -1) {
        const firstLine = Math.max(firstRelevantLine - 1, 0);
        return {
          start: firstLine,
          lines: functionLines.slice(firstLine, firstLine + 10)
        };
      } else {
        return {
          start: 1,
          lines: functionLines.slice(0, 10)
        };
      }
    } else {
      return {
        start: 0,
        lines: []
      };
    }
  }

  render() {
    const functionResult = this.getCurrentSelectionFunctionLines();
    return (
      <div className="box-action phn">
        <div className="container container-c">
          <h4 className="mvn">Search any action, data type, library or test in this skill</h4>

          <div className="columns">
            <div className="column column-one-half">
              <SearchWithResults
                ref={(el) => this.searchInput = el}
                placeholder={"Search by name, trigger or code"}
                value={this.state.currentResultValue}
                options={this.state.results}
                isSearching={false}
                noMatches={!this.state.hasMatches}
                onChangeSearch={this.onChangeSearchText}
                onSelect={this.onSelectResult}
                onEnterKey={this.onSelect}
                onEscKey={this.onEscKey}
              />
            </div>
            <div className="column column-one-half">
              <div className="display-nowrap display-overflow-hidden border pas type-monospace type-s">
                {functionResult.lines.map((ea, index) => (
                  <div key={`line${index}`}>
                    <span className="bg-light type-disabled mrs">{functionResult.start + index + 1}</span>
                    <pre className="display-inline">{ea}</pre>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <Button className="button-primary mrs" onClick={this.onSelect} disabled={!this.state.currentResultValue}>Switch to selection</Button>
          <Button onClick={this.onDone}>Cancel</Button>

        </div>
      </div>
    );
  }
}

export default QuickSearchPanel;
