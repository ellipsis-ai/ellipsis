/* eslint-env node */
/* Wraps a source file with amdefine's define function so RequireJS code can be
   used in node and jest */
const babel = require("babel-core");
const jestPreset = require("babel-preset-jest");
const reactPreset = require("babel-preset-react");

function prepend(src) {
  return "if (typeof define !== 'function') { var define = require('amdefine')(module); }\n" + src;
}

module.exports = {
  process: function(src, filename) {
    if (babel.util.canCompile(filename)) {
      const result = babel.transform(src, {
        filename,
        presets: [jestPreset, reactPreset],
        retainLines: true
      }).code;
      return prepend(result);
    }
    return src;
  }
};
