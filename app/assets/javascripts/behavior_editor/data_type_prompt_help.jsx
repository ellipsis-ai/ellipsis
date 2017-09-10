define(function(require) {
  const React = require('react'),
    Checklist = require('./checklist'),
    HelpPanel = require('../help/panel');

  class DataTypePromptHelp extends React.Component {

    render() {
      return (
        <HelpPanel
          heading="How to prompt the user"
          onCollapseClick={this.props.onCollapseClick}
        >
          <p>
          You can decide if Ellipsis should ask the user for input before running the function.
          </p>

          <Checklist>
            <Checklist.Item checkedWhen={!this.props.usesSearch}>
              <p>
                <span>Use <span className="type-semibold">Run the function first</span> if </span>
                <span>you always want to return a list of choices from your function, then let </span>
                <span>the user choose one. The function must always return one or more items.</span>
              </p>

            </Checklist.Item>

            <Checklist.Item checkedWhen={this.props.usesSearch}>
              <p>
                <span>Use <span className="type-semibold">Ask for user input first</span> if you want the user </span>
                <span>to enter text before the function runs. The function will receive a parameter named </span>
                <span><code>searchQuery</code>, which can be parsed or used to filter or search.</span>
              </p>

              <p>
                <span>The function may return a list of multiple matches, a single match, or no matches </span>
                <span>if the input was invalid. If the function returns no matches, the process will repeat: </span>
                <span>the user will be asked for input again, and the function will run again with the new text.</span>
              </p>
            </Checklist.Item>
          </Checklist>

        </HelpPanel>
      );
    }
  }

  DataTypePromptHelp.propTypes = {
    onCollapseClick: React.PropTypes.func.isRequired,
    usesSearch: React.PropTypes.bool.isRequired
  };

  return DataTypePromptHelp;
});
