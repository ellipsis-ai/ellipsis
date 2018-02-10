// @flow
import * as React from 'react';
import HelpPanel from '../help/panel';
import Constants from '../lib/constants';

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
            <span>Ellipsis can collect input from users by asking questions and waiting for the answers </span>
            <span>before running any code and completing a response. You can specify an answer be a specific type:</span>
          </p>

          <ul>
            <li><b>Text</b> (i.e. anything)</li>
            <li><b>Number</b></li>
            <li><b>Yes or no</b></li>
            <li><b>File</b> (e.g. a document or image)</li>
            <li>A choice from a list, where the list items are
              <ul>
                <li>retrieved from a <b>Data Type table</b>,</li>
                <li>or dynamically generated with <b>Data Type code</b></li>
              </ul>
            </li>
          </ul>

          <p>
            <span>Your action code will receive each answer in a function parameter using the input name. The inputs can also be repeated back in the response.</span>
          </p>

          <p>
            <span>Answers can also be saved for re-use, either for each user or for the whole team. If a saved answer is available, Ellipsis won’t repeat the question each time.</span>
          </p>

          <p>
            <a href={Constants.DOCS_URLS.INPUTS} target="help">Read more</a>
          </p>

        </HelpPanel>
      );
    }
}

export default UserInputHelp;
