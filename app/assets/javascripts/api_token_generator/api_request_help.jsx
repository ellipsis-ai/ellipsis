define(function(require) {
  var React = require('react'),
    Collapsible = require('../shared_ui/collapsible'),
    ExpandButton = require('../shared_ui/expand_button'),
    HelpPanel = require('../help/panel');

  return React.createClass({
    displayName: 'APIRequestHelp',
    propTypes: {
      onCollapse: React.PropTypes.func.isRequired
    },

    getInitialState: function() {
      return {
        activeSection: "postMessage"
      };
    },

    toggleActiveSection: function(sectionName) {
      const newSectionName = this.state.activeSection === sectionName ? "" : sectionName;
      this.setState({
        activeSection: newSectionName
      });
    },

    togglePostMessage: function() {
      this.toggleActiveSection("postMessage");
    },

    toggleSay: function() {
      this.toggleActiveSection("say");
    },

    collapse: function() {
      this.props.onCollapse();
    },

    getApiUrl(methodName) {
      return jsRoutes.controllers.APIController[methodName]().absoluteURL();
    },

    render: function() {
      return (
        <div>
          <HelpPanel
            heading="Ellipsis API methods"
            onCollapseClick={this.collapse}
          >

            <div className="border-bottom pbl">
              <h4 className="mvn">
                <ExpandButton onToggle={this.togglePostMessage} expandedWhen={this.state.activeSection === "postMessage"}>
                  <code>post_message</code>
                  <span className="type-weak"> — trigger a response from Ellipsis in a channel</span>
                </ExpandButton>
              </h4>

              <Collapsible revealWhen={this.state.activeSection === "postMessage"}>
                <div className="mtl">
                  <p><code>POST</code> to <code className="box-code-example">{this.getApiUrl("postMessage")}</code></p>

                  <h5>Parameters</h5>
                  <ul>
                    <li><code className="box-code-example">message:</code> a message which would normally trigger a
                      skill
                    </li>
                    <li><code className="box-code-example">responseContext:</code> use <code
                      className="box-code-example">slack</code> to see a response in Slack
                    </li>
                    <li><code className="box-code-example">channel:</code> the name of the channel to send a response
                    </li>
                    <li><code className="box-code-example">token:</code> a valid API token</li>
                  </ul>
                </div>
              </Collapsible>
            </div>

            <div className="border-bottom pvl">
              <h4 className="mvn">
                <ExpandButton onToggle={this.toggleSay} expandedWhen={this.state.activeSection === "say"}>
                  <code>say</code>
                  <span className="type-weak"> — get Ellipsis to repeat some text in a channel</span>
                </ExpandButton>
              </h4>

              <Collapsible revealWhen={this.state.activeSection === "say"}>
                <div className="mtl">
                  <p><code>POST</code> to <code className="box-code-example">{this.getApiUrl("say")}</code></p>

                  <h5>Parameters</h5>
                  <ul>
                    <li><code className="box-code-example">message:</code> what you want Ellipsis to say</li>
                    <li><code className="box-code-example">responseContext:</code> use <code
                      className="box-code-example">slack</code> to see a response in Slack
                    </li>
                    <li><code className="box-code-example">channel:</code> the name of the channel to send the message
                    </li>
                    <li><code className="box-code-example">token:</code> a valid API token</li>
                  </ul>
                </div>
              </Collapsible>

            </div>

          </HelpPanel>
        </div>
      );
    }
  });
});
