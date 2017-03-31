define(function(require) {
  var React = require('react'),
    Collapsible = require('./collapsible');

  return React.createClass({
    displayName: 'PageNotification',
    propTypes: {
      name: React.PropTypes.string.isRequired,
      content: React.PropTypes.node.isRequired,
      onDismiss: React.PropTypes.func.isRequired,
      isDismissed: React.PropTypes.bool
    },

    dismiss: function() {
      this.props.onDismiss(this.props.name);
    },

    render: function() {
      return (
        <div>
          <Collapsible revealWhen={!this.props.isDismissed} animateInitialRender={true}>
            <div className="bg-pink-medium pvm type-white">
              <div className="container container-c">
                <div className="mhl">
                  <span className="mrm">
                    {this.props.content}
                  </span>
                  <button type="button"
                    className="button-s button-shrink button-inherit"
                    onClick={this.dismiss}
                  >
                    OK
                  </button>
                </div>
              </div>
            </div>
          </Collapsible>
        </div>
      );
    }
  });
});
