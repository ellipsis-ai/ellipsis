define(function(require) {
  var React = require('react'),
    Input = require('../form/input');

  return React.createClass({
    displayName: 'BehaviorNameInput',
    propTypes: {
      name: React.PropTypes.string.isRequired,
      onChange: React.PropTypes.func.isRequired,
      placeholder: React.PropTypes.string.isRequired
    },

    render: function() {
      return (
        <div className="columns container container-wide">
          <div className="column column-one-third mobile-column-full">
            <Input
              className="form-input-borderless form-input-l type-bold"
              ref="input"
              value={this.props.name}
              placeholder={this.props.placeholder}
              onChange={this.props.onChange}
            />
          </div>
        </div>
      );
    }
  });
});
