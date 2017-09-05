function ellipsisErrorCallback(err) {
  let callbackError;
  if (err instanceof Error) {
    callbackError = err;
  } else {
    const throwableError = new EllipsisError(err);
    Error.captureStackTrace(throwableError, ellipsisErrorCallback);
    callbackError = throwableError;
  }
  callback(null, {
    error: {
      name: callbackError.name,
      message: callbackError.message,
      userMessage: callbackError.userMessage,
      stack: callbackError.stack
    }
  });
}
