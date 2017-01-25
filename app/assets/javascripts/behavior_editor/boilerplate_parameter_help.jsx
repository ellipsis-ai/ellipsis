define(function(require) {
var React = require('react'),
  Collapsible = require('../shared_ui/collapsible'),
  HelpPanel = require('../help/panel');

return React.createClass({
  propTypes: {
    envVariableNames: React.PropTypes.arrayOf(React.PropTypes.string),
    apiAccessTokens: React.PropTypes.arrayOf(React.PropTypes.object),
    onAddNewEnvVariable: React.PropTypes.func.isRequired,
    onCollapseClick: React.PropTypes.func.isRequired
  },
  getInitialState: function() {
    return {
      expandedItem: null
    };
  },
  getExpandedItem: function() {
    return this.state.expandedItem;
  },
  expandedItemMatches: function(itemName) {
    return this.getExpandedItem() === itemName;
  },
  toggleExpandedItem: function(itemName) {
    this.setState({ expandedItem: this.getExpandedItem() === itemName ? null : itemName });
  },
  toggleSuccessExamples: function() {
    this.toggleExpandedItem('success');
  },
  toggleErrorExamples: function() {
    this.toggleExpandedItem('error');
  },
  toggleEnvVariables: function() {
    this.toggleExpandedItem('envVariables');
  },
  toggleApiAccessTokens: function() {
    this.toggleExpandedItem('apiAccessTokens');
  },
  renderExpandLabelFor: function(labelText, itemName) {
    return (
      <span>
        <span className="display-inline-block" style={{ width: '0.8em' }}>
          {this.expandedItemMatches(itemName) ? "▾" : "▸"}
        </span>
        <span> {labelText}</span>
      </span>
    );
  },
  renderEnvVarList: function() {
    var vars = this.props.envVariableNames;
    if (vars.length > 0) {
      return vars.map((name, index) => (
        <div key={`envVar${index}`}>
          <code>{name}:</code>
          <span className="bg-dark-translucent plxxxl mls"
            title="For security, environment variable values are not displayed."/>
        </div>
      ));
    } else {
      return (
        <p className="man">There are no environment variables configured.</p>
      );
    }
  },
  renderApiAccessTokenList: function() {
    var tokenList = this.props.apiAccessTokens;
    if (tokenList.length > 0) {
      return tokenList.map((token, index) => (
        <div key={`accessToken${index}`} className="mbs">
          <code className="box-code-example">{token.keyName}</code> — {token.displayName}
        </div>
      ));
    } else {
      return (
        <p className="man">This skill has no API access tokens available.</p>
      );
    }
  },
  render: function() {
    return (
      <HelpPanel
        heading="Available methods and properties"
        onCollapseClick={this.props.onCollapseClick}
      >
        <p>
          <span>The function will automatically receive the <code className="type-bold">ellipsis</code> object, which contains </span>
          <span>important methods and properties.</span>
        </p>

        <div className="columns columns-elastic">
          <div className="column-group">
            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>{"ellipsis \u007B"}</pre></div>
              <div className="column column-expand pbl"></div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  success(successResult)</pre></div>
              <div className="column column-expand pbl">
                <div>
                  <span>Ends the function, passing <span className="type-monospace type-bold">successResult</span> to </span>
                  <span>the response template and then displaying it to the user. </span>
                  <span><span className="type-monospace type-bold">successResult</span> can be a string, array, </span>
                  <span>or object. </span>
                  <button type="button" className="button-raw" onClick={this.toggleSuccessExamples}>
                    {this.renderExpandLabelFor('Examples', 'success')}
                  </button>
                </div>
                <Collapsible revealWhen={this.expandedItemMatches('success')}>
                  <div className="box-code-example mvs">
                    {'ellipsis.success("The answer is: " + answer);'}
                  </div>
                  <div className="box-code-example mvs">
                    {"ellipsis.success(\u007B firstName: 'Abraham', lastName: 'Lincoln' \u007D);"}
                  </div>
                  <div className="box-code-example mvs">
                    {"ellipsis.success(['Mercury', 'Venus', 'Earth', 'Mars', 'Jupiter', 'Saturn', 'Neptune', 'Uranus']);"}
                  </div>
                </Collapsible>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  error(message)</pre></div>
              <div className="column column-expand pbl">
                <div>
                  <span>Ends the function by showing an error message to the user. </span>
                  <span><span className="type-monospace type-bold">message</span> should be a string. </span>
                  <button type="button" className="button-raw" onClick={this.toggleErrorExamples}>
                    {this.renderExpandLabelFor('Example', 'error')}
                  </button>
                </div>
                <Collapsible revealWhen={this.expandedItemMatches('error')}>
                  <div className="box-code-example mvs">
                    {'ellipsis.error("There was a problem with your request.");'}
                  </div>
                </Collapsible>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  noResponse()</pre></div>
              <div className="column column-expand pbl">
                <span>Ends the function without sending a response.</span>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  env</pre></div>
              <div className="column column-expand pbl">
                <div>
                  <span>Contains any configured <b>environment variables</b> as properties, accessible by name. </span>
                  <button type="button" className="button-raw" onClick={this.toggleEnvVariables}>
                    {this.renderExpandLabelFor('Show list', 'envVariables')}
                  </button>
                </div>
                <Collapsible revealWhen={this.expandedItemMatches('envVariables')}>
                  <div className="phs pvxs bg-blue-lightest type-weak border border-blue position-relative mbs">
                    {this.renderEnvVarList()}
                  </div>
                  <button type="button" className="button-s" onClick={this.props.onAddNewEnvVariable}>
                    Add new environment variable
                  </button>
                </Collapsible>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  accessTokens</pre></div>
              <div className="column column-expand pbl">
                <div>
                  <span>Contains any <b>third-party API access tokens</b> available to the skill. </span>
                  <button type="button" className="button-raw" onClick={this.toggleApiAccessTokens}>
                    {this.renderExpandLabelFor('Show list', 'apiAccessTokens')}
                  </button>
                </div>
                <Collapsible revealWhen={this.expandedItemMatches('apiAccessTokens')}>
                  {this.renderApiAccessTokenList()}
                </Collapsible>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>  AWS</pre></div>
              <div className="column column-expand pbl">
                <span>Contains properties and methods of the <b>Amazon Web Services</b> (AWS) SDK, if in use. </span>
                <a href="http://docs.aws.amazon.com/AWSJavaScriptSDK/guide/node-intro.html" target="_blank">Help</a>
              </div>
            </div>

            <div className="column-row">
              <div className="column column-shrink pbl prxl"><pre>{"\u007D"}</pre></div>
              <div className="column column-expand pbl"></div>
            </div>
          </div>
        </div>

      </HelpPanel>
    );
  }
});

});
