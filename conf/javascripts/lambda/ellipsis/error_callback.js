/* global log:false */
function ellipsisErrorCallback(err, promise) {
  let callbackError;
  if (err instanceof Error) {
    callbackError = err;
  } else {
    const throwableError = new $CONTEXT_PARAM.Error(err);
    Error.captureStackTrace(throwableError, $CONTEXT_PARAM.error);
    callbackError = throwableError;
  }
  callback(null, {
    error: callbackError,
    logs: log
  });
}
