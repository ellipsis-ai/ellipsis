define(function(require) {
  var React = require('react'),
    Input = require('../form/input'),
    SectionHeading = require('./section_heading');

  return React.createClass({
    displayName: 'DataTypeNameInput',
    propTypes: {
      name: React.PropTypes.string.isRequired,
      onChange: React.PropTypes.func.isRequired
    },

    render: function() {
      return (
        <div className="container ptm">
          <SectionHeading number="1">Name of data type</SectionHeading>
          <div className="mbm">
            <Input
              className="form-input-borderless form-input-l"
              ref="input"
              value={this.props.name}
              placeholder="Give data type a name"
              onChange={this.props.onChange}
            />
          </div>
        </div>
      );
    }
  });
});
