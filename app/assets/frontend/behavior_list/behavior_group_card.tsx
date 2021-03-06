import * as React from 'react';
import BehaviorGroup from '../models/behavior_group';
import Button from "../form/button";
import autobind from "../lib/autobind";
import BehaviorVersion from "../models/behavior_version";
import SubstringHighlighter from '../shared_ui/substring_highlighter';
import EditableName from './editable_name';

type Props = {
  group: BehaviorGroup,
  onMoreInfoClick: (group: BehaviorGroup) => void,
  isImporting?: Option<boolean>,
  wasReimported?: Option<boolean>,
  cardClassName?: Option<string>,
  secondaryActions: any,
  searchText: string
}

class BehaviorGroupCard extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    autobind(this);
  }

  isImporting(): boolean {
    return Boolean(this.props.isImporting);
  }

  toggleMoreInfo(): void {
    this.props.onMoreInfoClick(this.props.group);
  }

  highlight(text: Option<string>) {
    if (text) {
      return (
        <SubstringHighlighter text={text} substring={this.props.searchText}/>
      );
    } else {
      return null;
    }
  }

  getDescriptionOrMatchingTriggers(group: BehaviorGroup) {
    var lowercaseDescription = group.getDescription().toLowerCase();
    var lowercaseSearch = this.props.searchText.toLowerCase();
    var matchingBehaviorVersions: Array<BehaviorVersion> = [];
    if (lowercaseSearch) {
      matchingBehaviorVersions = group.behaviorVersions.filter((version) => version.includesText(lowercaseSearch));
    }
    if (!lowercaseSearch || lowercaseDescription.includes(lowercaseSearch) || matchingBehaviorVersions.length === 0) {
      return this.highlight(group.description);
    } else {
      return (
        <div>
          {matchingBehaviorVersions.map((version, index) => (
            <EditableName
              className="mbs"
              version={version}
              disableLink={true}
              key={`matchingBehaviorVersion${version.behaviorId || version.exportId || index}`}
              highlightText={this.props.searchText}
            />
          ))}
        </div>
      );
    }
  }

  renderSecondaryAction() {
    return this.props.secondaryActions;
  }

  getDescription() {
    return (
      <div className="display-overflow-fade-bottom" style={{maxHeight: "4rem"}}>
        {this.getDescriptionOrMatchingTriggers(this.props.group)}
      </div>
    );
  }

  getMoreInfoText(): string {
    const triggerableActionCount = this.props.group.getActions().filter((ea) => ea.triggers.length > 0).length;
    if (triggerableActionCount === 0) {
      return "More info";
    } else if (triggerableActionCount === 1) {
      return "1 action";
    } else {
      return `${triggerableActionCount} actions`;
    }
  }

  getName() {
    return this.highlight(this.props.group.name) || (
      <span className="type-italic type-disabled">Untitled skill</span>
    );
  }

  renderIcon() {
    const icon = this.props.group.icon;
    if (icon) {
      return (
        <span style={{width: "1em"}} className="display-inline-block mrm type-icon">{icon}</span>
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
            {this.renderSecondaryAction()}
          </div>
        </div>
      </div>
    );
  }
}

export default BehaviorGroupCard;
