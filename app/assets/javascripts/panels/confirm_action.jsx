define(function(require) {
  var React = require('react'),
    DynamicLabelButton = require('../form/dynamic_label_button');

  return React.createClass({
    displayName: 'ConfirmActionPanel',
    propTypes: {
      children: React.PropTypes.node.isRequired,
      confirmText: React.PropTypes.node,
      confirmingText: React.PropTypes.node,
      cancelText: React.PropTypes.node,
      onCancelClick: React.PropTypes.func.isRequired,
      onConfirmClick: React.PropTypes.func.isRequired,
      isConfirming: React.PropTypes.bool
    },

    getConfirmButtonLabels: function() {
      var labels = [{
        text: this.props.confirmText || "OK",
        displayWhen: !this.props.isConfirming
      }];
      if (this.props.confirmingText) {
        labels.push({
          text: this.props.confirmingText,
          displayWhen: this.props.isConfirming
        });
      }
      return labels;
    },

    onCancelClick: function() {
      this.props.onCancelClick();
    },

    render: function() {
      return (
        <div className="box-action">
          <div className="container phn">
            <div>
              {this.props.children}
            </div>
            <div className="mtl">
              <DynamicLabelButton className="mrs mbs"
                disabledWhen={this.props.isConfirming}
                onClick={this.props.onConfirmClick}
                labels={this.getConfirmButtonLabels()}
              />
              <button type="button"
                className="button-primary mbs"
                onClick={this.onCancelClick}
                disabled={this.props.isConfirming}>
                {this.props.cancelText || "Cancel"}
              </button>
            </div>
          </div>
        </div>
      );
    }
  });
});
