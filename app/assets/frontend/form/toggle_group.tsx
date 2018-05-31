import * as React from 'react';
import autobind from "../lib/autobind";

interface ToggleGroupProps {
  children: any
  className?: Option<string>
}

class ToggleGroup extends React.PureComponent<ToggleGroupProps> {
  static Item: typeof ToggleGroupItem;

  render() {
    return (
      <div className={"form-toggle-group " + (this.props.className || "")}>
        {this.props.children}
      </div>
    );
  }
}

interface ToggleGroupItemProps {
  activeWhen: boolean,
  label: React.ReactNode,
  onClick: () => void,
  title?: Option<string>
}

class ToggleGroupItem extends React.PureComponent<ToggleGroupItemProps> {
  button: Option<HTMLButtonElement>;

  constructor(props: ToggleGroupItemProps) {
    super(props);
    autobind(this);
  }

  onClick() {
    this.props.onClick();
    setTimeout(() => {
      if (this.button) {
        this.button.blur();
      }
    }, 1)
  }

  render() {
    return (
      <button
        ref={(el) => this.button = el}
        type="button" className={
        "button-toggle " +
        (this.props.activeWhen ? " button-toggle-active " : "")
      } onClick={this.onClick}
        title={this.props.title || ""}
      >{this.props.label}</button>
    );
  }
}

ToggleGroup.Item = ToggleGroupItem;

export default ToggleGroup;
export {ToggleGroup, ToggleGroupItem};
