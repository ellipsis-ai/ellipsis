import * as React from 'react';
import Event, {AnyKeyboardEvent} from '../lib/event';
import autobind from "../lib/autobind";

export interface DropdownMenuProps {
  children: React.ReactNode,
  labelClassName?: Option<string>,
  label: React.ReactChild,
  onDownArrow?: Option<() => void>,
  onUpArrow?: Option<() => void>,
  openWhen: boolean,
  menuClassName?: Option<string>,
  toggle: () => void
}

class DropdownMenu extends React.Component<DropdownMenuProps> {
  static Item: typeof DropdownMenuItem;
  button: Option<HTMLButtonElement>;
  menuItems: Array<Option<HTMLLIElement>>;
  container: Option<HTMLDivElement>;

  constructor(props: DropdownMenuProps) {
    super(props);
    autobind(this);
    this.button = null;
    this.container = null;
    this.menuItems = [];
  }

  toggle(): void {
    this.props.toggle();
  }

  onMouseDown(): void {
    this.toggle();
  }

  // Next two handlers needed to prevent clicks bubbling to the document which
  // might close an open dropdown

  onClick(event: MouseEvent): void {
    event.stopPropagation();
  }

  onItemClick(event: MouseEvent): void {
    event.stopPropagation();
  }

  onItemMouseUp(): void {
    this.toggle();
    this.focus();
  }

  onKeyDown(event: KeyboardEvent): void {
    if (Event.keyPressWasEnter(event) || Event.keyPressWasSpace(event)) {
      this.toggle();
    } else if (Event.keyPressWasUp(event) && this.props.onUpArrow) {
      this.props.onUpArrow();
      event.preventDefault();
    } else if (Event.keyPressWasDown(event) && this.props.onDownArrow) {
      this.props.onDownArrow();
      event.preventDefault();
    }
  }

  onItemKeyDown(event: KeyboardEvent): void {
    this.onKeyDown(event);
  }

  blur(): void {
    if (this.button) {
      this.button.blur();
    }
  }

  focus(): void {
    if (this.button) {
      this.button.focus();
    }
  }

  componentDidMount(): void {
    // Add click events the old-fashioned way so that propagation up to the document
    // can be stopped. (React events don't bubble up outside of React.)
    if (this.button) {
      this.button.addEventListener('click', this.onClick, false);
      this.button.addEventListener('keydown', this.onKeyDown, false);
    }
    this.menuItems.forEach((item) => {
      if (item) {
        item.addEventListener('click', this.onItemClick, false);
        item.addEventListener('keydown', this.onItemKeyDown, false);
      }
    });
  }

  render() {
    // "container" ref is used for testing
    return (
      <div ref={(el) => this.container = el} className="display-inline-block">
        <button type="button"
          className={
            "button-dropdown-trigger " +
            (this.props.openWhen ? " button-dropdown-trigger-menu-open " : "") +
            (this.props.labelClassName || "")
          }
          ref={(el) => this.button = el}
          onMouseDown={this.onMouseDown}
        >
          {this.props.label}
        </button>
        <div className="popup-dropdown-container position-relative">
          <ul className={
            "type-s popup popup-dropdown-menu " +
            (this.props.menuClassName || "") +
            (this.props.openWhen ? " fade-in " : " display-none ")
          }>
            {React.Children.map(this.props.children, (child, index) => {
              if (child) {
                const props = typeof child !== "string" && typeof child !== "number" ? child.props : null;
                return (
                  <li ref={"menuItem" + index} onMouseUp={this.onItemMouseUp} className={props.className || ""}>
                    {child}
                  </li>
                );
              } else {
                return null;
              }
            })}
          </ul>
        </div>
      </div>
    );
  }
}

export interface DropdownMenuItemProps {
  checkedWhen?: Option<boolean>,
  label: React.ReactChild,
  onClick?: () => void,
  className?: Option<string>
}

interface MenuItemState {
  hover: boolean
}

class DropdownMenuItem extends React.Component<DropdownMenuItemProps, MenuItemState> {
  constructor(props: DropdownMenuItemProps) {
    super(props);
    autobind(this);
    this.state = {
      hover: false
    };
  }

  onMouseEnter(): void {
    this.setState({
      hover: true
    });
  }

  onMouseLeave(): void {
    this.setState({
      hover: false
    });
  }

  onMouseUp(): void {
    if (this.props.onClick) {
      this.props.onClick();
    }
  }

  onKeyPress(event: AnyKeyboardEvent): void {
    if (Event.keyPressWasEnter(event) || Event.keyPressWasSpace(event)) {
      this.onMouseUp();
    }
  }

  shouldComponentUpdate(nextProps: DropdownMenuItemProps, nextState: MenuItemState): boolean {
    return this.props.checkedWhen !== nextProps.checkedWhen ||
      this.props.label !== nextProps.label ||
      this.state.hover !== nextState.hover;
  }

  visibleWhen(condition) {
    return " visibility " + (condition ? "visibility-visible" : "visibility-hidden") + " ";
  }

  render() {
    return (
      <button
        type="button"
        className={"button-dropdown-item " + (this.state.hover ? "button-dropdown-item-hover" : "")}
        onMouseUp={this.onMouseUp}
        onKeyPress={this.onKeyPress}
        onMouseEnter={this.onMouseEnter}
        onMouseLeave={this.onMouseLeave}
      >
        <div className="columns columns-elastic">
          {typeof(this.props.checkedWhen) !== 'undefined' ? (
            <div className={"column column-shrink prs align-m " + this.visibleWhen(this.props.checkedWhen)}>
              ✓
            </div>
          ) : null}
          <div className={"column column-expand align-m " + (this.props.checkedWhen ? "type-semibold" : "")}>
            {this.props.label}
          </div>
        </div>
      </button>
    );
  }
}

DropdownMenu.Item = DropdownMenuItem;

export default DropdownMenu;
export {DropdownMenu, DropdownMenuItem};
