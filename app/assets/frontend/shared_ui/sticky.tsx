import * as React from 'react';
import DeepEqual from '../lib/deep_equal';

function setStyles(element: HTMLElement | null, styles: Partial<CSSStyleDeclaration>) {
  if (element && element.style) {
    Object.keys(styles).forEach((styleName) => {
      element.style[styleName] = styles[styleName];
    });
  }
}

export interface Coords {
  top: number,
  left: number,
  bottom: number
}

export interface Dimensions {
  width: number,
  height: number
}

type Props = {
  onGetCoordinates: () => Coords,
  children: any,
  disabledWhen?: boolean,
  innerClassName?: string,
  outerClassName?: string,
  windowDimensions: Dimensions
}

class Sticky extends React.Component<Props> {
  scrollTopPosition: number;
  lastCoords: Coords | null;
  innerContainer: HTMLElement | null;
  outerContainer: HTMLElement | null;
  placeholder: HTMLElement | null;

  constructor(props: Props) {
    super(props);
    this.scrollTopPosition = 0;
  }

  resetCoordinates() {
      if (!this.innerContainer) {
        return;
      }

      var coords = this.props.onGetCoordinates();
      if (DeepEqual.isEqual(coords, this.lastCoords)) {
        return;
      }

      this.lastCoords = coords;

      setStyles(this.placeholder, { width: "" });
      setStyles(this.outerContainer, { height: "" });
      setStyles(this.innerContainer, {
        position: "static",
        width: "",
        maxHeight: ""
      });

      var newWidth = this.innerContainer.clientWidth;

      if (!this.props.disabledWhen) {
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

      window.addEventListener('resize', this.resetCoordinates, false);
      this.resetCoordinates();
    }

    componentWillUpdate() {
      this.scrollTopPosition = this.innerContainer ? this.innerContainer.scrollTop : 0;
    }

    componentDidUpdate(prevProps) {
      if (this.props.disabledWhen && prevProps.disabledWhen) {
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
