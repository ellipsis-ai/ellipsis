import * as React from 'react';
import autobind from '../lib/autobind';
import BehaviorGroup from "../models/behavior_group";
import BehaviorVersion from "../models/behavior_version";
import Editable from "../models/editable";
import Button from "../form/button";
import SearchWithResults, {SearchOption} from "../form/search_with_results";
import Sort from "../lib/sort";
import SubstringHighlighter from "../shared_ui/substring_highlighter";

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

interface FunctionSearchResult {
  start: number,
  lines: Array<string>
}

const LINES_TO_SHOW = 8;

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
      action.functionBody.toLowerCase().includes(searchText)
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

  getCurrentSelectionFunctionLines(): FunctionSearchResult {
    const id = this.state.currentResultValue;
    const editable = this.props.group.behaviorVersions.find((ea) => ea.getPersistentId() === id) ||
      this.props.group.libraryVersions.find((ea) => ea.getPersistentId() === id);
    if (editable) {
      const functionLines = editable.functionBody.split("\n");
      const firstRelevantLine = functionLines.findIndex((line) => line.toLowerCase().includes(this.state.currentSearchValue.toLowerCase()));
      if (firstRelevantLine !== -1) {
        const firstLine = Math.max(firstRelevantLine - 1, 0);
        return {
          start: firstLine,
          lines: functionLines.slice(firstLine, firstLine + LINES_TO_SHOW)
        };
      } else {
        return {
          start: 1,
          lines: functionLines.slice(0, LINES_TO_SHOW)
        };
      }
    } else {
      return {
        start: 0,
        lines: []
      };
    }
  }

  preserveSpaces(text: string): string {
    return text.replace(/\s/g, "\u00A0");
  }

  renderFunctionLine(line: string, index: number, start: number) {
    return (
      <div key={`line${index}`}>
        <code>
          <span className="bg-light type-disabled mrs display-inline-block width-2 align-r phxs">{start + index + 1}</span>
          <span><SubstringHighlighter substring={this.preserveSpaces(this.state.currentSearchValue)} text={this.preserveSpaces(line)} /></span>
        </code>
      </div>
    );
  }

  renderEmptyLines(linesNeeded: number) {
    return Array(linesNeeded).fill("").map((ea, index) => (
      <div key={`emptyLine${index}`}>
        <code>
          <span className="bg-light type-disabled mrs display-inline-block width-2 align-r phxs">&nbsp;</span>
          <span>&nbsp;</span>
        </code>
      </div>
    ));
  }

  render() {
    const functionResult = this.getCurrentSelectionFunctionLines();
    const linesNeeded = LINES_TO_SHOW - functionResult.lines.length;
    return (
      <div className="box-action phn">
        <div className="container container-c">
          <h4 className="mvn">Search any action, data type, library or test in this skill</h4>

          <div>
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
              fullWidth={true}
            />
          </div>
          <div className="mbl display-nowrap display-overflow-hidden border border-radius type-xs">
            {functionResult.lines.map((ea, index) => this.renderFunctionLine(ea, index, functionResult.start))}
            {this.renderEmptyLines(linesNeeded)}
          </div>

          <Button className="button-primary mrs" onClick={this.onSelect} disabled={!this.state.currentResultValue}>Switch to selection</Button>
          <Button onClick={this.onDone}>Cancel</Button>

        </div>
      </div>
    );
  }
}

export default QuickSearchPanel;
