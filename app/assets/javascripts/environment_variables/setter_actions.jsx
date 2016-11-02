define(function(require) {
  var React = require('react');

  return React.createClass({
    displayName: 'SetterActions',
    propTypes: {
      isFullPage: React.PropTypes.bool,
      children: React.PropTypes.node.isRequired
    },

    render: function() {
      if (this.props.isFullPage) {
        return (
          <footer className="position-fixed-bottom position-z-front border-top bg-white">
            <div className="container pts">
              <div className="columns">
                <div className="column column-one-quarter"></div>
                <div className="column column-three-quarters plxxxxl">
                  {this.renderChildren()}
                </div>
              </div>
            </div>
          </footer>
        );
      } else {
        return (
          <div className="mtxl">
            {this.renderChildren()}
          </div>
        );
      }
    },

    renderChildren: function() {
      return React.Children.map(this.props.children, (child) => child);
    }
  });
});
