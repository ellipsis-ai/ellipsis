define(function(require) {
  var React = require('react'),
    Input = require('../models/input');

  return React.createClass({
    displayName: 'SharedAnswerInputSelector',
    propTypes: {
      onToggle: React.PropTypes.func.isRequired,
      onSelect: React.PropTypes.func.isRequired,
      inputs: React.PropTypes.arrayOf(React.PropTypes.instanceOf(Input))
    },

    onSelectInput: function(input) {
      this.props.onSelect(input);
      this.props.onToggle();
    },

    render: function() {
      return (
        <div>
          <div className="box-action phn">
            <div className="container">
              <div className="columns">
                <div className="column column-page-sidebar">
                  <h4 className="type-weak">Select a saved answer to re-use</h4>
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
  });
});
