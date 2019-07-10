import * as React from 'react';
import * as ReactDOM from "react-dom";
import autobind from '../../lib/autobind';
import SkillManifest from "./index";
import Page from "../../shared_ui/page";
import {Timestamp} from "../../lib/formatter";
import User, {UserJson} from "../../models/user";

interface Props {
  containerId: string
  csrfToken: string
  isAdmin: boolean
  teamId: string
  items: Array<SkillManifestItemJson>
}

declare var SkillManifestConfig: Props;

export type SkillManifestDevelopmentStatus = "Production" | "Development" | "Requested"

export interface SkillManifestItemJson {
  name: string
  id: Option<string>
  editor: Option<UserJson>
  description: string
  managed: boolean
  created: Timestamp
  firstDeployed: Option<Timestamp>
  lastUsed: Option<Timestamp>
}

export interface SkillManifestItem extends SkillManifestItemJson {
  editor: Option<User>
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
          items={this.props.items.map((ea) => Object.assign({}, ea, {
            editor: ea.editor ? User.fromJson(ea.editor) : null
          }))}
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
