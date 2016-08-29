define(function(require) {
var React = require('react'),
  HelpPanel = require('../help/panel');

return React.createClass({
  propTypes: {
    onCollapseClick: React.PropTypes.func.isRequired
  },
  render: function() {
    return (
      <HelpPanel
        heading="Configure Ellipsis to integrate with your AWS account"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>In order for Ellipsis behaviors to access your AWS account, you need to configure credentials. </span>
          <span>Such credentials are stored using “environment variables” (separate from individual behaviors), </span>
          <span>so you only need to add them once.</span>
        </p>

        <p>
          <span>It is recommended to create a new user for Ellipsis in the </span>
          <span><b>Identity and Access Management</b> section of your AWS console, and then </span>
          <span>create an access key for that user.</span>
        </p>

        <p>
          <span>Credentials include: </span>
        </p>

        <ol>
          <li>the <b>access key ID</b></li>
          <li>the <b>secret access key</b></li>
          <li>the <b>region</b> of your AWS account (e.g. <code>us-east-1</code>)</li>
        </ol>

        <p className="mbn">
          <span>For more information about AWS access keys, consult </span>
          <a href="http://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html?icmpid=docs_iam_console" target="_blank">
            the AWS user guide
          </a>
          <span>.</span>
        </p>
      </HelpPanel>
    );
  }
});

});
