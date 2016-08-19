define(function(require) {
  var React = require('react');

  return React.createClass({
    propTypes: {
      apis: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
      csrfToken: React.PropTypes.string.isRequired,
      teamId: React.PropTypes.string.teamId
    },

    render: function() {
      return (
        <div>
          <div className="bg-light">
            <div className="container pbm">
              <h3 className="mvn ptxxl type-weak display-ellipsis">
                <span>API applications</span>
                <span>→</span>
                <span>Add an application</span>
              </h3>
            </div>
          </div>

          <div className="container ptxl pbxxxl">
            {this.props.apis.map(function(api) {
              return api.name;
            }, this).join(', ')}
          </div>
        </div>
      );
    }
  });
});
