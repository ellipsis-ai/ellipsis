import * as React from 'react';
import autobind from '../lib/autobind';
import * as debounce from "javascript-debounce";
import Sticky, {Coords} from "../shared_ui/sticky";

interface Props {
  groupId: string
  activePage: ConfigPage
  children: React.ReactNode
  onSidebarWidthChange: (width: number) => void
  headerHeight: number
  footerHeight: number
  isMobile: boolean
}

type ConfigPage = "scheduling"

class BehaviorGroupConfigPage extends React.Component<Props> {
  sidebar: Option<HTMLDivElement>;
  sidebarWidth: number;
  delayUpdate: () => void;

  constructor(props: Props) {
    super(props);
    autobind(this);
    this.sidebarWidth = 0;
    this.delayUpdate = debounce(this.updateSidebarWidth, 50);
  }

  componentDidMount(): void {
    this.updateSidebarWidth();
    window.addEventListener('resize', this.delayUpdate);
  }

  componentDidUpdate(): void {
    this.updateSidebarWidth();
  }

  updateSidebarWidth(): void {
    const width = this.sidebar && !this.props.isMobile ? this.sidebar.offsetWidth : 0;
    if (width !== this.sidebarWidth) {
      this.sidebarWidth = width;
      this.props.onSidebarWidthChange(this.sidebarWidth);
    }
  }

  activeClassWhenPageName(pageName: ConfigPage): string {
    return this.props.activePage === pageName ? "list-nav-active-item" : "";
  }

  getCoordsForSidebar(): Coords {
    return {
      top: this.props.headerHeight,
      left: 0,
      bottom: window.innerHeight - this.props.headerHeight - this.props.footerHeight
    }
  }

  render() {
    return (
      <div className="flex-row-cascade">
        <div className="flex-columns flex-row-expand">
          <div className="flex-column flex-column-left flex-rows">
            <div className="columns flex-columns flex-row-expand mobile-flex-no-columns">
              <div ref={(el) => this.sidebar = el}
                className="column column-page-sidebar flex-column flex-column-left bg-white border-right border-light prn"
              >
                <Sticky onGetCoordinates={this.getCoordsForSidebar} disabledWhen={this.props.isMobile}>
                  <nav className="container">
                    <ul className="list-nav">
                      <li className="mbxl">
                        <h5>Skill configuration</h5>
                      </li>
                      <li className={this.activeClassWhenPageName("scheduling")}>
                        <a href={jsRoutes.controllers.BehaviorGroupConfigController.schedules(this.props.groupId).url}>Scheduling</a>
                      </li>
                    </ul>
                  </nav>
                </Sticky>
              </div>
              <div className="column column-page-main column-page-main-wide flex-column flex-column-left phn">
                {this.props.children}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default BehaviorGroupConfigPage;
