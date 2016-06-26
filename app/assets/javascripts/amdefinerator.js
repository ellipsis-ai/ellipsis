/* eslint-env node */
/* Wraps a source file with amdefine's define function so RequireJS code can be
   used in node and jest */
var babelJest = require("babel-jest");

function prepend(src) {
  return "if (typeof define !== 'function') { var define = require('amdefine')(module); }\n" + src;
}

module.exports = {
  process: function(src, filename) {
    var result = babelJest.process(src, filename);
    return src.match(/^define\(/) ? prepend(result) : result;
  }
};
