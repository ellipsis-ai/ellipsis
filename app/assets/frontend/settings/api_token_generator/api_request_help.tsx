import * as React from 'react';
import Collapsible from '../../shared_ui/collapsible';
import ExpandButton from '../../shared_ui/expand_button';
import HelpPanel from '../../help/panel';
import autobind from "../../lib/autobind";

interface Props {
  onCollapse: () => void
}

interface State {
  activeSection: string
}

class APIRequestHelp extends React.PureComponent<Props, State> {
    constructor(props: Props) {
      super(props);
      autobind(this);
      this.state = {
        activeSection: "postMessage"
      }
    }

    toggleActiveSection(sectionName: string): void {
      const newSectionName = this.state.activeSection === sectionName ? "" : sectionName;
      this.setState({
        activeSection: newSectionName
      });
    }

    togglePostMessage(): void {
      this.toggleActiveSection("postMessage");
    }

    toggleSay(): void {
      this.toggleActiveSection("say");
    }

    collapse(): void {
      this.props.onCollapse();
    }

    getApiUrl(methodName: keyof APIController): string {
      const method = jsRoutes.controllers.api.APIController[methodName];
      return method().absoluteURL(true);
    }

    render() {
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
                  <span className="type-weak"> — run an action in a channel using a trigger message</span>
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
}

export default APIRequestHelp;
