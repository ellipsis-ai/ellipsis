const util = require('util');
const builtInConsole = Object.assign({}, console);
class LogEntry {
  constructor(args, stack) {
    this.logged = args.map(LogEntry.inspect).join(" ");
    this.stack = stack;
  }
  static inspect(item) {
    return typeof item === "string" ? item : util.inspect(item);
  }
}
const EllipsisConsole = {
  logs: [],
  write: function ellipsisConsole(consoleMethod, realArgs, caller) {
    const args = [].slice.call(realArgs);
    const error = { toString: () => consoleMethod };
    Error.captureStackTrace(error, caller);
    builtInConsole[consoleMethod].apply(null, args);
    EllipsisConsole.logs.push(new LogEntry(args, error.stack));
  }
};

const lambdaCallback = callback;
callback = function ellipsisCallback(err, result) {
  lambdaCallback(err, Object.assign(result, {
    logs: EllipsisConsole.logs
  }));
};
console.log = function consoleLog() { EllipsisConsole.write("log", arguments, consoleLog); };
console.error = function consoleError() { EllipsisConsole.write("error", arguments, consoleError); };
console.warn = function consoleWarn() { EllipsisConsole.write("warn", arguments, consoleWarn); };
console.info = function consoleInfo() { EllipsisConsole.write("info", arguments, consoleInfo); };
