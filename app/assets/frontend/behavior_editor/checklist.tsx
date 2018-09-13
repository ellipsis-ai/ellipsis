import * as React from 'react';

interface ChecklistProps {
  children: React.ReactNode,
  className?: Option<string>,
  disabledWhen?: Option<boolean>
}

class Checklist extends React.Component<ChecklistProps> {
  static Item: typeof ChecklistItem;
    render() {
      return (
        <ul className={
          "type-s list-space-s checklist " +
          (this.props.disabledWhen ? " type-weak " : "") +
          (this.props.className || "")
        }>
          {this.props.children}
        </ul>
      );
    }
}

interface ChecklistItemProps {
  children: React.ReactNode,
  checkedWhen?: Option<boolean>,
  hiddenWhen?: Option<boolean>
}

class ChecklistItem extends React.Component<ChecklistItemProps> {
    render() {
      return (
        <li className={
          (this.props.checkedWhen ? " checklist-checked " : "") +
          (this.props.hiddenWhen ? " display-none " : " fade-in ")
        }>
          {this.props.children}
        </li>
      );
    }
}

Checklist.Item = ChecklistItem;

export default Checklist;
export {Checklist, ChecklistItem}

