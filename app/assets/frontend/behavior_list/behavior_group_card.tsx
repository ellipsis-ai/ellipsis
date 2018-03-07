import * as React from 'react';
import BehaviorGroup from '../models/behavior_group';
import Checkbox from '../form/checkbox';
import SVGInstall from '../svg/install';
import SVGInstalled from '../svg/installed';
import SVGInstalling from '../svg/installing';
import Button from "../form/button";
import autobind from "../lib/autobind";

type Props = {
  groupData: BehaviorGroup,
  localId?: string | null,
  description?: any,
  name?: any,
  icon?: string | null,
  onBehaviorGroupImport?: (BehaviorGroup) => void,
  onMoreInfoClick: (BehaviorGroup) => void,
  isImportable: boolean,
  isImporting?: boolean | null,
  onCheckedChange?: (localId: string, isChecked: boolean) => void,
  isChecked?: boolean | null,
  wasReimported?: boolean | null,
  cardClassName?: string | null
}

class BehaviorGroupCard extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  isImportable(): boolean {
    return this.props.isImportable;
  }

  isImporting(): boolean {
    return Boolean(this.props.isImporting);
  }

  isLocallyEditable(): boolean {
    return Boolean(this.props.localId);
  }

  importBehavior(): void {
    if (this.props.onBehaviorGroupImport) {
      this.props.onBehaviorGroupImport(this.props.groupData);
    }
  }

  toggleMoreInfo(): void {
    this.props.onMoreInfoClick(this.props.groupData);
  }

  renderSecondaryAction() {
    if (!this.isImportable() && !this.isImporting() && this.isLocallyEditable()) {
      return (
        <Checkbox
          className="display-block type-s"
          onChange={this.onCheckedChange}
          checked={this.props.isChecked}
          label="Select"
        />
      );
    } else if (this.isImporting()) {
      return (
        <Button title="Installing, please wait…" className="button-raw button-no-wrap height-xl" disabled={true}
          onClick={() => {
          }}>
          <span className="display-inline-block align-m mrs" style={{width: 40, height: 24}}><SVGInstalling/></span>
          <span className="display-inline-block align-m">
              Installing…
            </span>
        </Button>
      );
    } else if (this.isLocallyEditable()) {
      return (
        <Button title="Already installed" className="button-raw button-no-wrap height-xl" disabled={true}
          onClick={() => {
          }}>
          <span className="display-inline-block align-m mrs" style={{width: 40, height: 24}}><SVGInstalled/></span>
          <span className="display-inline-block align-m type-green">
              Installed
            </span>
        </Button>
      );
    } else {
      return (
        <Button title="Install this skill" className="button-raw button-no-wrap height-xl"
          onClick={this.importBehavior}>
          <span className="display-inline-block align-m mrs" style={{width: 40, height: 24}}><SVGInstall/></span>
          <span className="display-inline-block align-m">
              Install
            </span>
        </Button>
      );
    }
  }

  renderWasReimported() {
    if (this.props.wasReimported) {
      return (
        <div className="type-s align-r fade-in">
          <span className="display-inline-block align-m mrs" style={{width: 30, height: 18}}><SVGInstalled/></span>
          <span className="display-inline-block align-m type-green">Re-installed</span>
        </div>
      );
    } else {
      return null;
    }
  }

  getDescription() {
    return (
      <div className="display-overflow-fade-bottom" style={{maxHeight: "4rem"}}>
        {this.props.description}
      </div>
    );
  }

  getMoreInfoText(): string {
    var actionCount = this.props.groupData.behaviorVersions.filter((ea) => !ea.isDataType()).length;
    if (actionCount === 0) {
      return "More info";
    } else if (actionCount === 1) {
      return "1 action";
    } else {
      return `${actionCount} actions`;
    }
  }

  onCheckedChange(isChecked: boolean): void {
    if (this.props.onCheckedChange && this.props.localId) {
      this.props.onCheckedChange(this.props.localId, isChecked);
    }
  }

  getName() {
    return this.props.name || (
      <span className="type-italic type-disabled">Untitled skill</span>
    );
  }

  renderIcon() {
    if (this.props.icon) {
      return (
        <span style={{width: "1em"}} className="display-inline-block mrm type-icon">{this.props.icon}</span>
      );
    } else {
      return null;
    }
  }

  render() {
    return (
      <div className={"border border-radius position-relative " + (this.props.cardClassName || "")}>
        <div className={this.isImporting() ? "pulse" : ""}>
          <div className="phl pvm border-bottom border-light">
            <Button className="button-block width-full" onClick={this.toggleMoreInfo} disabled={this.isImporting()}>
              <div className="type-l display-ellipsis mbm" style={{height: "1.7778rem"}}>
                {this.renderIcon()}
                {this.getName()}
              </div>
              <div className="type-s display-overflow-hidden" style={{height: "5.3334rem"}}>
                <div>{this.getDescription()}</div>
                <div>
                  <span className={this.isImporting() ? "type-disabled" : "link"}>{this.getMoreInfoText()}</span>
                </div>
              </div>
            </Button>
          </div>
          <div className="phl pvm width" style={{height: "2.6667rem"}}>
            <div className="columns">
              <div className="column column-one-half">{this.renderSecondaryAction()}</div>
              <div className="column column-one-half">{this.renderWasReimported()}</div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default BehaviorGroupCard;
