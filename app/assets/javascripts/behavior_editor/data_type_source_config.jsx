define(function(require) {
  const React = require('react'),
    Constants = require('../lib/constants'),
    SectionHeading = require('../shared_ui/section_heading'),
    autobind = require('../lib/autobind');

  class DataTypeSourceConfig extends React.Component {
    constructor(props) {
      super(props);
      autobind(this);
    }

    onUseDefaultStorage() {
      this.props.onChange(false);
    }

    onUseCode() {
      this.props.onChange(true);
    }

    render() {
      return (
        <div className="container ptxl pbxxxl">
          <p>
            Data types are used to limit a user’s input to a particular list of items.
          </p>

          <SectionHeading number="1">Where should the list come from?</SectionHeading>

          <div className="columns">
            <div className="column column-one-half border-right pvm prxxl">
              <div className="mbxl">
                <button type="button" onClick={this.onUseDefaultStorage}>Data stored by Ellipsis</button>
              </div>

              <ul className="list-space-l type-s">
                <li>
                  <span>Choose this to create a table stored by Ellipsis.</span>
                </li>

                <li>
                  <span>You define what columns the table should have, and you can add/delete rows as needed.</span>
                </li>

                <li>
                  <span>You can also query and alter the data later from your skill using </span>
                  <span><a href={Constants.GRAPHQL_DOCS_URL} target="_blank">GraphQL</a> code.</span>
                </li>

                <li>
                  <span>Example usages:</span>
                  <ul>
                    <li>A list of choices that can be created or modified by other actions</li>
                    <li>A list of choices that doesn’t change frequently</li>
                  </ul>
                </li>
              </ul>

            </div>
            <div className="column column-one-half border-left mlneg1 pvm plxxl">

              <div className="mbxl">
                <button type="button" onClick={this.onUseCode}>Data returned by code</button>
              </div>

              <ul className="list-space-l type-s">
                <li>
                  Choose this to write a Node.js function that returns a list of items.
                </li>

                <li>
                  You can tell Ellipsis to ask for user input you can use to search or filter the result.
                </li>

                <li>
                  The function will run each time an action runs that uses this data type for input.
                </li>

                <li>
                  <span>Example usages: </span>
                  <ul>
                    <li>fetching a list of items from an external API</li>
                    <li>generating a list based on user input or other runtime data</li>
                  </ul>
                </li>
              </ul>

            </div>
          </div>
        </div>
      );
    }
  }

  DataTypeSourceConfig.propTypes = {
    onChange: React.PropTypes.func.isRequired
  };

  return DataTypeSourceConfig;
});
