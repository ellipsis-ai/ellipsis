import * as React from 'react';
import CollapseButton from '../shared_ui/collapse_button';
import autobind from "../lib/autobind";

interface Props {
  children: React.ReactNode,
  heading: React.ReactChild,
  onCollapseClick: () => void
}

class HelpPanel extends React.Component<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  render() {
    return (
      <div className="box-help type-s ptn mobile-position-fixed-bottom-full container container-wide mbneg1">
        <div className="columns">
          <div className="column column-page-sidebar mobile-display-none" />
          <div className="column column-page-main position-relative">
            <div className="position-absolute position-top-right">
              <CollapseButton onClick={this.props.onCollapseClick} direction="down" />
            </div>
          </div>
        </div>
        <div className="columns">
          <div className="column column-page-sidebar mtl mobile-prxl">
            {typeof this.props.heading === "string" ? (
              <h4 className="mtn type-weak">
                {this.props.heading}
              </h4>
            ) : this.props.heading}
          </div>
          <div className="column column-page-main prxl">
            <div className="mtl mobile-mtn">
              {this.props.children}
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default HelpPanel;
