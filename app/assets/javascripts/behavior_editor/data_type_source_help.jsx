define(function(require) {
  const React = require('react'),
    Constants = require('../lib/constants'),
    HelpPanel = require('../help/panel');

  class DataTypeSourceHelp extends React.Component {

    render() {
      return (
        <HelpPanel
          heading="Data type source"
          onCollapseClick={this.props.onCollapseClick}
        >
          <p>
            <span>Each data type must have a source: either data that is stored by Ellipsis in a table, </span>
            <span>or data returned by code (a Node.js function).</span>
          </p>

          <h5>Data stored by Ellipsis</h5>
          <p>
            <span>Choose this to create a data table stored by Ellipsis. </span>
            <span>You define what columns the table should have, and you can add/delete rows as needed.</span>
          </p>

          <p>
            <span>The table can also be queried or altered later with </span>
            <span><a href={Constants.GRAPHQL_DOCS_URL} target="_blank">GraphQL</a> in other actions or </span>
            <span>data types that use code.</span>
          </p>

          <p>
            <span>Example usages:</span>
          </p>

          <ul>
            <li>A list of items that can be created or modified by other actions</li>
            <li>A list of items that doesn’t change frequently</li>
          </ul>

          <h5>Data returned by code</h5>

          <p>
            <span>Choose this to write a Node.js function that returns a list of one or more items. </span>
            <span>The function will run each time this data type is used for input.</span>
          </p>

          <p>
            <span>If desired, you can tell Ellipsis to ask for user input first that can be used to parse, search or filter.</span>
          </p>

          <p>
            <span>Example usages: </span>
          </p>

          <ul>
            <li>fetching a list of items from an external API</li>
            <li>filtering a list based on user input</li>
            <li>filtering a list from a data type stored by Ellipsis</li>
            <li>transforming user input into structured data</li>
            <li>generating a dynamic list using run-time data</li>
          </ul>
        </HelpPanel>
      );
    }
  }

  DataTypeSourceHelp.propTypes = {
    onCollapseClick: React.PropTypes.func.isRequired
  };

  return DataTypeSourceHelp;
});
