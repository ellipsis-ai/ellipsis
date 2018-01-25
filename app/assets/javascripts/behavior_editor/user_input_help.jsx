// @flow
define(function(require) {
  const React = require('react'),
    HelpPanel = require('../help/panel'),
    Constants = require('../lib/constants');

  type Props = {
    onCollapseClick: () => void
  }

  class UserInputHelp extends React.PureComponent<Props> {
    props: Props;
    render() {
      return (
        <HelpPanel
          heading="Collecting user input"
          onCollapseClick={this.props.onCollapseClick}
        >
          <p>
            <span>Ellipsis can collect input from users by asking questions and waiting for all of the answers </span>
            <span>before running any code or completing a response. Answers can be restricted if desired to specific types:</span>
          </p>

          <ul>
            <li>numbers</li>
            <li>yes or no</li>
            <li>choices from a list, where the items are
              <ul>
                <li>retrieved from a table, or</li>
                <li>sourced dynamically from code</li>
              </ul>
            </li>
          </ul>

          <p>
            <span>Your function will receive each input as a parameter by name. The inputs can also be output in the response.</span>
          </p>

          <p>
            <span>Answers can also be saved for re-use, either for each user or for the whole team.</span>
          </p>

          <p>
            <a href={Constants.DOCS_URLS.INPUTS} target="help">Read more</a>
          </p>

        </HelpPanel>
      );
    }
  }

  return UserInputHelp;

});
