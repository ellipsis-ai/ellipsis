import * as React from 'react';
import autobind from '../../lib/autobind';
import {PageRequiredProps} from "../../shared_ui/page";

type Props = PageRequiredProps & {
  csrfToken: string
  isAdmin: boolean
  teamId: string
}

class SkillManifest extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  getMainColumnStyle(): React.CSSProperties {
    return {
      paddingBottom: this.props.footerHeight
    };
  }

  render() {
    return (
      <div className="flex-row-cascade">
        <div className="flex-columns flex-row-expand">
          <div className="flex-columns flex-row-expand">
            <div className="flex-column flex-column-left flex-rows container container-wide phn">
              <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
                <div className="column column-page-sidebar flex-column flex-column-left bg-lightest mobile-border-bottom prn">
                  <nav className="mvxxl plxl">
                    <ul className="list-nav">
                      <li><a href={jsRoutes.controllers.DashboardController.usage(this.props.isAdmin ? this.props.teamId : null).url}>Usage report</a></li>
                      <li className="list-nav-active-item">Skill manifest</li>
                    </ul>
                  </nav>
                </div>
                <div
                  className="column column-page-main column-page-main-wide flex-column flex-column-main position-relative bg-white"
                  style={this.getMainColumnStyle()}
                >
                  Hello world
                </div>
              </div>
            </div>
          </div>
        </div>
        {this.props.onRenderFooter()}
      </div>
    );
  }
}

export default SkillManifest;
