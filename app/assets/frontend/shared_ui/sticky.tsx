import * as React from 'react';
import DeepEqual from '../lib/deep_equal';

interface ValidCSSProps {
  width: string,
  height: string,
  position: string,
  maxHeight: string,
  overflowY: string,
  top: string,
  left: string
}

function setStyles(element: Option<HTMLElement>, styles: Partial<ValidCSSProps>) {
  const keys = Object.keys(styles) as Array<keyof ValidCSSProps>;
  keys.forEach((styleName) => {
    const value = styles[styleName];
    if (element && value) {
      element.style.setProperty(styleName, value);
    }
  });
}

export interface Coords {
  top: number,
  left: number,
  bottom: number
}

type Props = {
  onGetCoordinates: () => Coords,
  children: any,
  disabledWhen?: boolean | (() => boolean),
  innerClassName?: string,
  outerClassName?: string
}

class Sticky extends React.Component<Props> {
  scrollTopPosition: number;
  lastCoords: Option<Coords>;
  innerContainer: Option<HTMLElement>;
  outerContainer: Option<HTMLElement>;
  placeholder: Option<HTMLElement>;
  windowWidth: number;

  constructor(props: Props) {
    super(props);
    this.scrollTopPosition = 0;
    this.windowWidth = window.innerWidth;
  }

  isDisabledFor(props: Props): boolean {
    if (typeof props.disabledWhen === "boolean") {
      return props.disabledWhen;
    } else if (typeof props.disabledWhen === "function") {
      return props.disabledWhen();
    } else {
      return false;
    }
  }

  resetCoordinates() {
      if (!this.innerContainer) {
        return;
      }

      var coords = this.props.onGetCoordinates();
      if (DeepEqual.isEqual(coords, this.lastCoords) && this.windowWidth === window.innerWidth) {
        return;
      }

      this.windowWidth = window.innerWidth;
      this.lastCoords = coords;

      setStyles(this.placeholder, { width: "" });
      setStyles(this.outerContainer, { height: "" });
      setStyles(this.innerContainer, {
        position: "static",
        width: "",
        maxHeight: ""
      });

      var newWidth = this.innerContainer.clientWidth;

      if (!this.isDisabledFor(this.props)) {
        setStyles(this.outerContainer, {
          height: `${coords.bottom}px`
        });
        setStyles(this.innerContainer, {
          top: `${coords.top}px`,
          left: `${coords.left}px`,
          width: `${newWidth}px`,
          maxHeight: `${coords.bottom}px`,
          position: 'fixed'
        });

        setStyles(this.placeholder, { width: `${newWidth}px` });
      }

      this.innerContainer.scrollTop = this.scrollTopPosition;
    }

    componentDidMount() {
      setStyles(this.innerContainer, {
        overflowY: 'auto'
      });

      window.addEventListener('resize', () => this.resetCoordinates(), false);
      this.resetCoordinates();
    }

    componentWillUpdate() {
      this.scrollTopPosition = this.innerContainer ? this.innerContainer.scrollTop : 0;
    }

    componentDidUpdate(prevProps: Props) {
      if (this.isDisabledFor(this.props) && this.isDisabledFor(prevProps)) {
        return;
      }
      this.resetCoordinates();
    }

    render() {
      return (
        <div className={this.props.outerClassName || ""} style={{ position: "relative" }} ref={(div) => { this.outerContainer = div; }}>
          <div ref={(div) => { this.placeholder = div; }} />
          <div className={this.props.innerClassName || ""} ref={(div) => { this.innerContainer = div; }}>
            {this.props.children}
          </div>
        </div>
      );
    }
}

export default Sticky;
