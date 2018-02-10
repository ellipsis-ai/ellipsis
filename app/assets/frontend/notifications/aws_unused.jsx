import * as React from 'react';

const NotificationForUnusedAWS = React.createClass({
    propTypes: {
      details: React.PropTypes.arrayOf(React.PropTypes.shape({
        code: React.PropTypes.string.isRequired
      })).isRequired
    },

    render: function() {
      return (
        <span>
          <span>Use <code className="box-code-example mhxs">{this.props.details[0].code}</code> in your </span>
          <span>function to access methods and properties of the </span>
          <span><a href="http://docs.aws.amazon.com/AWSJavaScriptSDK/guide/node-intro.html" target="_blank" rel="noreferrer noopener">AWS SDK</a>.</span>
        </span>
      );
    }
});

export default NotificationForUnusedAWS;
