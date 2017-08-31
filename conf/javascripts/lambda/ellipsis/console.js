/* global log:false */
(function() {
const util = require('util');
class Log {
  constructor(args, stack) {
    this.logged = args.map(Log.inspect).join(" ");
    this.stack = stack;
  }
  static inspect(item) {
    return typeof item === "string" ? item : util.inspect(item);
  }
}
const builtInConsole = Object.assign({}, console);
function augmentConsole(consoleMethod, realArgs, caller) {
  const args = [].slice.call(realArgs);
  const error = { toString: () => consoleMethod };
  Error.captureStackTrace(error, caller);
  builtInConsole[consoleMethod].apply(null, args);
  log.push(new Log(args, error.stack));
}
console.log = function consoleLog() { augmentConsole("log", arguments, consoleLog); };
console.error = function consoleError() { augmentConsole("error", arguments, consoleError); };
console.warn = function consoleWarn() { augmentConsole("warn", arguments, consoleWarn); };
console.info = function consoleInfo() { augmentConsole("info", arguments, consoleInfo); };
})();
