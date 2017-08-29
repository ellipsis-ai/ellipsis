const builtInConsole = Object.assign({}, console);
function augmentConsole(consoleMethod, realArgs, caller) {
  const args = [].slice.call(realArgs);
  const error = { toString: () => consoleMethod };
  Error.captureStackTrace(error, caller);
  const newArgs = error.stack.split("\\n").length > 1 ?
    args.concat("\\nELLIPSIS_STACK_TRACE_START\\n" + error.stack + "\\nELLIPSIS_STACK_TRACE_END") :
    args;
  builtInConsole[consoleMethod].apply(null, newArgs);
}
console.log = function consoleLog() { augmentConsole("log", arguments, consoleLog); };
console.error = function consoleError() { augmentConsole("error", arguments, consoleError); };
console.warn = function consoleWarn() { augmentConsole("warn", arguments, consoleWarn); };
console.info = function consoleInfo() { augmentConsole("info", arguments, consoleInfo); };
