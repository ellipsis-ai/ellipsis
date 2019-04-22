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
  items: Array<SkillManifestItem>
}

declare var SkillManifestConfig: Props;

export type SkillManifestDevelopmentStatus = "Production" | "Development" | "Requested"

export interface SkillManifestItem {
  name: string
  id: Option<string>
  editor: string
  description: string
  active: boolean
  developmentStatus: SkillManifestDevelopmentStatus
  managed: boolean
  lastUsed: string
}

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
          items={this.props.items}
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
