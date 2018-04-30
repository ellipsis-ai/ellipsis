type HSL = {
  h: number,
  s: number,
  l: number
}

type RGB = {
  r: number,
  g: number,
  b: number
}

  class RgbaColor {
    r: number;
    g: number;
    b: number;
    a: number;

    constructor(r: number, g: number, b: number, a: Option<number>) {
      this.r = r;
      this.g = g;
      this.b = b;
      this.a = typeof a === "number" ? a : 1;
    }

    hasAlpha(): boolean {
      return typeof (this.a) === "number" && this.a < 1;
    }

    numberToPaddedHex(n: number): string {
      const str = n.toString(16);
      return str.length === 1 ? `0${str}` : str;
    }

    toHex(): string {
      const hex = `#${this.numberToPaddedHex(this.r)}${this.numberToPaddedHex(this.g)}${this.numberToPaddedHex(this.b)}`;
      if (this.hasAlpha()) {
        return hex + this.numberToPaddedHex(Math.round(this.a * 255));
      } else {
        return hex;
      }
    }

    toRGBA(): string {
      const rgb = `${this.r}, ${this.g}, ${this.b}`;
      if (this.hasAlpha()) {
        return `rgba(${rgb}, ${this.a.toFixed(2)})`;
      } else {
        return `rgb(${rgb})`;
      }
    }

    toHSLA(): string {
      const hsl = RgbaColor.rgbToHsl(this.r, this.g, this.b);
      const hslString = `${hsl.h}, ${hsl.s}%, ${hsl.l}%`;
      if (this.hasAlpha()) {
        return `hsla(${hslString}, ${this.a.toFixed(2)})`;
      } else {
        return `hsl(${hslString})`;
      }
    }

    static fromCSS(colorValue: string): Option<RgbaColor> {
      let r, g, b, a;
      const rgbMatch = colorValue.match(/rgba?\s*\(\s*(\d+)[\s,]*(\d+)[\s,]*(\d+)[\s,]*(\d+.?\d*)?/i);
      const hexMatch = colorValue.match(/#([0-9a-f]){3,6}/i);
      const hslMatch = colorValue.match(/hsla?\s*\(\s*(\d+)[\s,]*(\d+)%[\s,]*(\d+)%[\s,]*(\d+.?\d*)?/i);
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
      } else if (hslMatch) {
        const h = parseInt(hslMatch[1], 10);
        const s = parseInt(hslMatch[2], 10);
        const l = parseInt(hslMatch[3], 10);
        const maybeAlpha = parseFloat(hslMatch[4]);
        const rgb = RgbaColor.hslToRgb(h, s, l);
        r = rgb.r;
        g = rgb.g;
        b = rgb.b;
        a = isNaN(maybeAlpha) ? null : maybeAlpha;
      }
      if (typeof r === "number" && typeof g === "number" && typeof b === "number") {
        return new RgbaColor(r, g, b, a);
      } else {
        return null;
      }
    }

    static rgbToHsl(rInt: number, gInt: number, bInt: number): HSL {
      const r = rInt / 255;
      const g = gInt / 255;
      const b = bInt / 255;
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
          default:
            h = (r - g) / d + 4;
            break;
        }

        h /= 6;
      }
      return {
        h: Math.round(h * 360),
        s: Math.round(s * 100),
        l: Math.round(l * 100)
      };
    }

    static hslToRgb(hInt, sInt, lInt): RGB {
      let h = hInt / 360;
      let s = parseInt(sInt, 10) / 100;
      let l = parseInt(lInt, 10) / 100;
      let r, g, b;

      if (s === 0) {
        r = g = b = l; // achromatic
      } else {
        const hue2rgb = function(p, q, tOrig) {
          let t = tOrig;
          if (t < 0) t += 1;
          if (t > 1) t -= 1;
          if (t < 1 / 6) return p + (q - p) * 6 * t;
          if (t < 1 / 2) return q;
          if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
          return p;
        };

        var q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        var p = 2 * l - q;

        r = hue2rgb(p, q, h + 1 / 3);
        g = hue2rgb(p, q, h);
        b = hue2rgb(p, q, h - 1 / 3);
      }

      return {
        r: Math.round(r * 255),
        g: Math.round(g * 255),
        b: Math.round(b * 255)
      };
    }
  }

export default RgbaColor;
