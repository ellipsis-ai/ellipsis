import * as React from 'react';
import Collapsible from '../shared_ui/collapsible';
import * as Constants from '../lib/constants';
import HelpPanel from '../help/panel';
import autobind from "../lib/autobind";

interface Props {
  onCollapseClick: () => void
  libraryName?: Option<string>
}

interface State {
  expandedItems: Array<string>
}

class LibraryCodeHelp extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    autobind(this);
    this.state = {
      expandedItems: []
    };
  }

  getExpandedItems(): Array<string> {
    return this.state.expandedItems;
  }

  hasExpandedItem(itemName: string): boolean {
    return this.getExpandedItems().includes(itemName);
  }

  toggleExpandedItem(itemName: string): void {
    const hasExpanded = this.hasExpandedItem(itemName);
    const newItems = hasExpanded ?
      this.getExpandedItems().filter((ea) => ea !== itemName) :
      this.getExpandedItems().concat(itemName);
    this.setState({ expandedItems: newItems });
  }

  toggleLibraryExamples(): void {
    this.toggleExpandedItem('libraryExamples');
  }

  renderExpandLabelFor(labelText: string, itemName: string) {
    return (
      <span>
        <span className="display-inline-block" style={{ width: '0.8em' }}>
          {this.hasExpandedItem(itemName) ? "▾" : "▸"}
        </span>
        <span> {labelText}</span>
      </span>
    );
  }

  getRequireExample(): string {
    return `require("${this.props.libraryName || "library1"}")`;
  }

  render() {
    return (
      <HelpPanel
        heading="Library functions"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>Write JavaScript code compatible with </span>
          <span><a href={Constants.NODE_JS_DOCS_URL} target="_blank">Node.js {Constants.NODE_JS_VERSION}</a> that returns an object, function, or class you can use in your skill. </span>
        </p>

        <p>
          <span>You can <code>require()</code> any <a href="https://www.npmjs.com/">NPM package</a> or any other library in your skill.</span>
        </p>

        <p>
          <button type="button" className="button-raw" onClick={this.toggleLibraryExamples}>
            {this.renderExpandLabelFor("Show examples", "libraryExamples")}
          </button>
        </p>

        <Collapsible revealWhen={this.hasExpandedItem("libraryExamples")}>

          <p><b>Example: return an object with methods and properties:</b></p>
          <h6>Library code</h6>
          <pre className="box-code-example mvl">{
`return {
  getData(params) {
    return [1, 3, 5, 7, 11];
  },
  formatText(text) {
    return "You said: " + text;
  },
  version: "1.0"
};`
          }</pre>

          <h6>Using the library</h6>
          <pre className="box-code-example mvl">{
`const lib = ${this.getRequireExample()};
lib.formatText("item 1");`
          }</pre>

          <p><b>Example: return a single function:</b></p>
          <h6>Library code</h6>
          <pre className="box-code-example mvl">{
`return function formatText(text) {
  return "You said: " + text;
};`
          }</pre>

          <h6>Using the library</h6>
          <pre className="box-code-example mvl">{
`const formatText = ${this.getRequireExample()};
formatText("item1");`
          }</pre>

          <p><b>Example: return an ES6 class with constructor:</b></p>

          <h6>Library code</h6>
          <pre className="box-code-example mvl">{
`class Person {
  constructor(name) {
    this.name = name;
  }
}

Person.version = "1.0";

return Person;
`
          }</pre>

          <h6>Using the library</h6>
          <pre className="box-code-example mvl">{
`const Person = ${this.getRequireExample()};
const person = new Person("Winston Zeddemore");`
          }</pre>
        </Collapsible>
      </HelpPanel>
    );
  }
}

export default LibraryCodeHelp;
