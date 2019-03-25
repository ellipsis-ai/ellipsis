import * as React from 'react';
import Input from '../models/input';
import autobind from "../lib/autobind";

interface Props {
  onToggle: () => void,
  onSelect: (input: Input) => void,
  inputs: Array<Input>
}

class SharedAnswerInputSelector extends React.PureComponent<Props> {
    constructor(props: Props) {
      super(props);
      autobind(this);
    }

    onSelectInput(input: Input) {
      this.props.onSelect(input);
      this.props.onToggle();
    }

    render() {
      return (
        <div>
          <div className="box-action phn">
            <div className="container">
              <div className="columns">
                <div className="column column-page-sidebar">
                  <h4 className="mtn type-weak">Select a saved answer to re-use</h4>
                  <p className="type-weak type-s">
                    <span>You can re-use any input from another action that saves answers. Users </span>
                    <span>will only be asked to respond to such inputs once, with the answer saved for every action.</span>
                  </p>
                </div>
                <div className="column column-page-main">
                  <div className="columns columns-elastic type-s border-top">
                    {this.props.inputs.map((ea, index) => (
                      <div className="column-group" key={`sharedInput${index}`}>
                        <div className="column-row">
                          <div className="type-bold column column-shrink pvs border-bottom">
                            <button type="button"
                              className="button-raw type-monospace"
                              onClick={this.onSelectInput.bind(this, ea)}
                            >
                              {ea.name}
                            </button>
                          </div>
                          <div className="column column-expand pvs border-bottom type-weak type-italic">{ea.question}</div>
                        </div>
                      </div>
                    ))}
                  </div>

                  <div className="mtxl">
                    <button type="button" onClick={this.props.onToggle}>
                      Cancel
                    </button>
                  </div>

                </div>
              </div>
            </div>
          </div>
        </div>
      );
    }
}

export default SharedAnswerInputSelector;
