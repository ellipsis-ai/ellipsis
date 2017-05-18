define(function(require) {
  const React = require("react"),
    RgbaColor = require('./rgba_color');

  return React.createClass({
    getColorClasses: function() {
      const colorList = [].slice.call(document.styleSheets).reduce((all, styleSheet) => {
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
      // Find the unique set by name
      const byName = {};
      colorList.forEach((color) => {
        if (!byName[color.name]) {
          byName[color.name] = color;
        }
      });
      return Object.keys(byName).map((name) => byName[name]);
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
