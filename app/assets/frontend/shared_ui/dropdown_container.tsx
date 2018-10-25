import * as React from 'react';

interface Props {
  children: any
}

export const DROPDOWN_CONTAINER_CLASS_NAME = "popup-dropdown-container";

class DropdownContainer extends React.Component<Props> {
  static eventIsFromDropdown(event: Event): boolean {
    const target = event.target;
    if (target instanceof Element) {
      return Boolean(target.closest(`.${DROPDOWN_CONTAINER_CLASS_NAME}`));
    } else {
      return false;
    }
  }

  render() {
    return (
      <div className={`${DROPDOWN_CONTAINER_CLASS_NAME} position-relative`}>
        {this.props.children}
      </div>
    );
  }
}

export default DropdownContainer;
