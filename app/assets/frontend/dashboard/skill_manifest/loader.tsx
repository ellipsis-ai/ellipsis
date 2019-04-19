import * as React from 'react';
import * as ReactDOM from "react-dom";
import autobind from '../../lib/autobind';
import SkillManifest from "./index";
import Page from "../../shared_ui/page";

interface Props {
  containerId: string
  csrfToken: string
  isAdmin: boolean
  teamId: string
}

declare var SkillManifestConfig: Props;

class SkillManifestLoader extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <Page csrfToken={this.props.csrfToken} onRender={(pageProps) => (
        <SkillManifest
          csrfToken={this.props.csrfToken}
          isAdmin={this.props.isAdmin}
          teamId={this.props.teamId}
          {...pageProps}
        />
      )} />
    );
  }
}

const container = document.getElementById(SkillManifestConfig.containerId);
if (container) {
  ReactDOM.render((
    <SkillManifestLoader {...SkillManifestConfig} />
  ), container);
}
