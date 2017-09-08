define(function(require) {
var React = require('react'),
  Collapsible = require('../shared_ui/collapsible'),
  Constants = require('../lib/constants'),
  HelpPanel = require('../help/panel');

return React.createClass({
  propTypes: {
    onCollapseClick: React.PropTypes.func.isRequired,
    libraryName: React.PropTypes.string
  },
  getInitialState: function() {
    return {
      expandedItems: []
    };
  },
  getExpandedItems: function() {
    return this.state.expandedItems;
  },
  hasExpandedItem: function(itemName) {
    return this.getExpandedItems().includes(itemName);
  },
  toggleExpandedItem: function(itemName) {
    const hasExpanded = this.hasExpandedItem(itemName);
    const newItems = hasExpanded ?
      this.getExpandedItems().filter((ea) => ea !== itemName) :
      this.getExpandedItems().concat(itemName);
    this.setState({ expandedItems: newItems });
  },
  toggleLibraryExamples: function() {
    this.toggleExpandedItem('libraryExamples');
  },
  renderExpandLabelFor: function(labelText, itemName) {
    return (
      <span>
        <span className="display-inline-block" style={{ width: '0.8em' }}>
          {this.hasExpandedItem(itemName) ? "▾" : "▸"}
        </span>
        <span> {labelText}</span>
      </span>
    );
  },
  renderForAction: function() {
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

        <div className="box-code-example mvl">ellipsis.success("It worked!");</div>

        <div className="box-code-example mvl">ellipsis.noResponse();</div>
      </div>
    );
  },

  getRequireExample: function() {
    // require.js freaks out if there's a string with a require call in it
    return "require" + `("${this.props.libraryName || "library1"}")`;
  },

  render: function() {
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
  getData: function(params) {
    return [1, 3, 5, 7, 11];
  },
  formatText: function(text) {
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
});

});
