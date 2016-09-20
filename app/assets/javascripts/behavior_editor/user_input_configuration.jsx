define(function(require) {
  var React = require('react'),
    SectionHeading = require('./section_heading'),
    UserInputDefinition = require('./user_input_definition'),
    ifPresent = require('../if_present');

  return React.createClass({
    propTypes: {
      onParamChange: React.PropTypes.func.isRequired,
      onParamDelete: React.PropTypes.func.isRequired,
      onParamAdd: React.PropTypes.func.isRequired,
      onEnterKey: React.PropTypes.func.isRequired,
      userParams: React.PropTypes.arrayOf(React.PropTypes.shape({
        name: React.PropTypes.string.isRequired,
        question: React.PropTypes.string.isRequired,
        paramType: React.PropTypes.shape({
          name: React.PropTypes.string.isRequired
        }).isRequired
      })).isRequired,
      paramTypes: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
    },

    onChange: function(index, data) {
      this.props.onParamChange(index, data);
    },
    onDelete: function(index) {
      this.props.onParamDelete(index);
    },
    onEnterKey: function(index) {
      this.props.onEnterKey(index);
    },
    focusIndex: function(index) {
      this.refs['param' + index].focus();
    },

    render: function() {
      return (
        <div className="columns">
          <div className="column column-one-quarter mobile-column-full mts mbxxl mobile-mbs">
            <SectionHeading>Collect this input</SectionHeading>
          </div>
          <div className="column column-three-quarters mobile-column-full pll mobile-pln mbxxl">
            {ifPresent(this.props.userParams, (params) => (
              <div>
                <div className="mbm">
                  {params.map((param, paramIndex) => (
                    <div key={'paramInput' + paramIndex} className="columns columns-elastic mbxl">
                      <div className="column column-expand pll">
                        <UserInputDefinition
                          key={'UserInputDefinition' + paramIndex}
                          ref={'param' + paramIndex}
                          name={param.name}
                          paramTypes={this.props.paramTypes}
                          paramType={param.paramType}
                          question={param.question}
                          onChange={this.onChange.bind(this, paramIndex)}
                          onDelete={this.onDelete.bind(this, paramIndex)}
                          onEnterKey={this.onEnterKey.bind(this, paramIndex)}
                          id={paramIndex}
                        />
                      </div>
                    </div>
                  ))}
                </div>
                <div className="align-r prxs mobile-align-l mbs">
                  <button type="button" className="button-s" onClick={this.props.onParamAdd}>Add another input</button>
                </div>
              </div>
            ), () => (
              <div className="mvs">
                <button type="button" className="button-s" onClick={this.props.onParamAdd}>Add input</button>
              </div>
            ))}
        </div>
        </div>

      );
    }
  });
});
