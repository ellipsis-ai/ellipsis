define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        kind: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired
      })).isRequired
    },

    render: function() {
      var numParams = this.props.details.length;
      if (numParams === 1) {
        let detail = this.props.details[0];
        return (
          <span>
            <span><code className="type-bold">{`{${detail.name}}`}</code> is not a valid input label. </span>
            <span>Input labels can only include non-accented letters, numbers, dollar signs, or underscores, and can’t start with a number.</span>
          </span>
        );
      } else {
        return (
          <span>
            <span>Input labels can only include non-accented letters, numbers, dollar signs, or underscores, and can’t start with a number. </span>
            <span>These are not valid labels: </span>
            {this.props.details.map((detail, index) => (
              <span>
                <code className="mhxs" key={`invalidParamName${index}`}>{`{${detail.name}}`}</code>
                <span className="type-weak">{index + 1 < numParams ? " · " : ""}</span>
              </span>
            ))}
          </span>
        );
      }
    }
  });
});
