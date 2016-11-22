define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'PageHeading',
    propTypes: {
      heading: React.PropTypes.node.isRequired,
      children: React.PropTypes.node
    },

    render: function() {
      return (
        <div className="bg-light">
          <div className="container">
            <div className="columns">
              <div className="column column-one-quarter ptxl pbs">
                <h3 className="mvn type-weak">
                  {this.props.heading}
                </h3>
              </div>
              <div className="column column-three-quarters pll">
                {this.props.children}
              </div>
            </div>
          </div>
        </div>
      );
    }
  });
});
