function ellipsisErrorCallback(err, options) {
  let callbackError;
  if (err instanceof Error) {
    callbackError = err;
  } else {
    const throwableError = new EllipsisError(err, options);
    Error.captureStackTrace(throwableError, ellipsisErrorCallback);
    callbackError = throwableError;
  }
  callback(null, {
    error: {
      name: callbackError.name,
      message: callbackError.message,
      userMessage: callbackError.userMessage || options.userMessage,
      stack: callbackError.stack
    }
  });
}
