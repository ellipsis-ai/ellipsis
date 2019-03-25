import * as React from 'react';
import Collapsible from '../shared_ui/collapsible';
import * as Constants from '../lib/constants';
import HelpPanel from '../help/panel';
import autobind from "../lib/autobind";

interface Props {
  onCollapseClick: () => void,
  isDataType: boolean
}

interface State {
  expandedItems: Array<string>
}

class BehaviorCodeHelp extends React.Component<Props, State> {
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
  toggleDataTypeSuccessExamples(): void {
    this.toggleExpandedItem('dataTypeSuccess');
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

  renderForAction() {
    return (
      <div>
        <p>
          <span>The function will automatically receive the <code className="type-bold">ellipsis</code> </span>
          <span>object, which contains important methods and properties, along with any inputs </span>
          <span>you’ve defined in the action.</span>
        </p>

        <p>
          <span>Ensure your function calls the <code className="type-bold">success</code> or </span>
          <span><code className="type-bold">noResponse</code> response method to finish, for example:</span>
        </p>

        <div className="box-code-example mvs">
          {'ellipsis.success("The answer is: " + answer);'}
        </div>

        <div className="box-code-example mvs">
          {"ellipsis.success(\u007B firstName: 'Abraham', lastName: 'Lincoln' \u007D);"}
        </div>

        <div className="box-code-example mvs">
          {"ellipsis.success(['Mercury', 'Venus', 'Earth', 'Mars', 'Jupiter', 'Saturn', 'Neptune', 'Uranus']);"}
        </div>

        <div className="box-code-example mvl">ellipsis.noResponse();</div>
      </div>
    );
  }

  renderForDataType() {
    return (
      <div>
        <ul>
          <li>
            <span>The function will automatically receive the <code className="type-bold">ellipsis</code> </span>
            <span>object, which contains important methods and properties.</span>
          </li>
          <li>
            <span>If your data type requests input, your function will also receive a </span>
            <span><code className="type-bold">searchQuery</code> parameter.</span>
          </li>
        </ul>

        <p>
          <span>Your function should call <code className="type-bold">ellipsis.success</code> with an array </span>
          <span>of items to be shown to the user as a list of choices.</span>
        </p>

        <p>
          <span>Each item must have <code className="type-bold">id</code> and </span>
          <span><code className="type-bold">label</code> properties. <code>id</code> should be </span>
          <span>unique and the <code>label</code> will be shown to the user.</span>
        </p>

        <p>
          <button type="button" className="button-raw" onClick={this.toggleDataTypeSuccessExamples}>
            {this.renderExpandLabelFor("Show examples", "dataTypeSuccess")}
          </button>
        </p>

        <Collapsible revealWhen={this.hasExpandedItem("dataTypeSuccess")}>
          <p>
            The user will be asked to choose from 3 items:
          </p>

          <pre className="box-code-example mvl">{
  `ellipsis.success([
    { id: "A", label: "The letter A" },
    { id: "B", label: "The letter B" },
    { id: "C", label: "The letter C" }
  ]);`
          }</pre>

          <p>
            If you ask for user input first, you can return a single item if there's only one possible result:
          </p>

          <pre className="box-code-example mvl">{
  `ellipsis.success([
    { id: "A", label: "The letter A" }
  ]);`
          }</pre>

          <p>
            <span>If you ask for user input, and it was invalid or there are no matches, return an empty array. </span>
            <span>The user will be asked to provide new input and then the function will run again.</span>
          </p>

          <pre className="box-code-example mvl">{
  `ellipsis.success([]);`
          }</pre>
        </Collapsible>

      </div>
    );
  }

  render() {
    return (
      <HelpPanel
        heading={this.props.isDataType ? "Data type functions" : "Action functions"}
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>Write a JavaScript function compatible with </span>
          <span><a href={Constants.NODE_JS_DOCS_URL} target="_blank">Node.js {Constants.NODE_JS_VERSION}</a>. </span>
        </p>

        {this.props.isDataType ? this.renderForDataType() : this.renderForAction()}

        <p>
          <span>To indicate an error, use the <code className="type-bold">ellipsis.Error</code> class, for example:</span>
        </p>

        <div className="box-code-example mvl">{'throw new ellipsis.Error("error 404");'}</div>

        <div className="box-code-example mvl">{'throw new ellipsis.Error("error 404", { userMessage: "Something went wrong!" });'}</div>

        <p>
          <span>You can <code>require()</code> any <a href="https://www.npmjs.com/">NPM package</a> or any library you add to your skill.</span>
        </p>

        <pre className="box-code-example mvl">{'// Use the request npm module\nconst request = require("request");'}</pre>

        <h5>The ellipsis object</h5>
        <p>
          <span>The function will receive a parameter called <code>ellipsis</code>, an object with useful run-time properties. Use the editor’s autocomplete to explore them.</span>
        </p>

      </HelpPanel>
    );
  }
}

export default BehaviorCodeHelp;
