define(function(require) {
  const React = require("react");

  class RgbaColor {
    constructor(r, g, b, a) {
      this.r = r;
      this.g = g;
      this.b = b;
      this.a = a;
    }

    hasAlpha() {
      return typeof (this.a) === "number";
    }

    toHex() {
      const hex = `#${this.r.toString(16)}${this.g.toString(16)}${this.b.toString(16)}`;
      if (this.hasAlpha()) {
        return hex + Math.round(this.a * 256).toString(16);
      } else {
        return hex;
      }
    }

    toRGBA() {
      const rgb = `${this.r}, ${this.g}, ${this.b}`;
      if (this.hasAlpha()) {
        return `rgba(${rgb}, ${this.a.toFixed(2)})`;
      } else {
        return `rgb(${rgb})`;
      }
    }

    toHSLA() {
      const r = this.r / 255;
      const g = this.g / 255;
      const b = this.b / 255;
      const max = Math.max(r, g, b);
      const min = Math.min(r, g, b);
      let h, s, l;
      l = (max + min) / 2;
      if (max === min) {
        h = s = 0; // achromatic
      } else {
        const d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

        switch (max) {
          case r:
            h = (g - b) / d + (g < b ? 6 : 0);
            break;
          case g:
            h = (b - r) / d + 2;
            break;
          case b:
            h = (r - g) / d + 4;
            break;
        }

        h /= 6;
      }

      const hsl = `${Math.round(h * 360)}, ${Math.round(s * 100)}%, ${Math.round(l * 100)}%`;
      if (this.hasAlpha()) {
        return `hsla(${hsl}, ${this.a.toFixed(2)})`;
      } else {
        return `hsl(${hsl})`;
      }
    }

    static fromCSS(colorValue) {
      let r, g, b, a;
      const rgbMatch = colorValue.match(/rgba?\s*\(\s*(\d+)[\s,]*(\d+)[\s,]*(\d+)[\s,]*(\d+.?\d*)?/i);
      const hexMatch = colorValue.match(/#([0-9a-f]){3,6}/i);
      const hslMatch = colorValue.match(/hsla?\s*\(\s*(\d+)[\s,]*(\d+%)[\s,]*(\d+%)[\s,]*(\d+.?\d*)?/i);
      if (rgbMatch) {
        r = parseInt(rgbMatch[1], 10);
        g = parseInt(rgbMatch[2], 10);
        b = parseInt(rgbMatch[3], 10);
        const maybeAlpha = parseFloat(rgbMatch[4]);
        a = isNaN(maybeAlpha) ? null : maybeAlpha;
      } else if (hexMatch) {
        if (hexMatch[4] && hexMatch[5] && hexMatch[6]) {
          r = parseInt(hexMatch[1] + hexMatch[2], 16);
          g = parseInt(hexMatch[3] + hexMatch[4], 16);
          b = parseInt(hexMatch[5] + hexMatch[6], 16);
        } else {
          r = parseInt(hexMatch[1] + hexMatch[1], 16);
          g = parseInt(hexMatch[2] + hexMatch[2], 16);
          b = parseInt(hexMatch[3] + hexMatch[3], 16);
        }
      }
      return new RgbaColor(r, g, b, a);
    }
  }

  return React.createClass({
    getColorClasses: function() {
      return [].slice.call(document.styleSheets).reduce((all, styleSheet) => {
        if (styleSheet.cssRules) {
          return all.concat(
            [].slice.call(styleSheet.cssRules)
              .filter((ea) => {
                return ea.type === CSSRule.STYLE_RULE &&
                  ea.selectorText.includes(".color-");
              })
              .reduce((prev, cssRule) => {
                const colorName = cssRule.selectorText.replace(/\.color-/, "");
                const value = cssRule.style.getPropertyValue("color");
                return prev.concat({
                  className: cssRule.selectorText.replace(/^\./, ""),
                  name: colorName,
                  value: value,
                  rgba: RgbaColor.fromCSS(value)
                });
              }, [])
          );
        } else {
          return all;
        }
      }, []);
    },

    renderColor: function(color) {
      return (
        <div className="display-inline-block align-t mrxl mbneg1 border bg-lightest" key={color.name}>
          <div className="display-inline-block align-t width width-5 height-5 mrneg1 position-relative position-z-above">
            <div className={`${color.className}`}>
              <svg width="100%" height="100%" viewBox="0 0 60 60" className="align-b">
                <rect x="0" y="0" width="60" height="60" fill="currentColor"/>
              </svg>
            </div>
          </div>
          <div className="display-inline-block width width-15 height-5 align-t">
            <div className="type-xs type-monospace pas display-block height height-full">
              <b>@{color.name}</b><br />
              {color.rgba.toHex()}<br />
              {color.rgba.toRGBA()}<br />
              {color.rgba.toHSLA()}<br />
            </div>
          </div>
        </div>
      );
    },

    render: function() {
      return (
        <div>
          <div className="container">
            <h1 className="mbxl type-blue-faded">Ellipsis styleguide colors</h1>
          </div>

          <div className="bg-white">
            <div className="container pvxl">
              {this.getColorClasses().map(this.renderColor)}
            </div>
          </div>
        </div>
      );
    }
  });
});
