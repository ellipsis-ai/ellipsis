function ellipsisErrorCallback(err) {
  if (err instanceof Error) {
    throw err;
  } else {
    const throwableError = new Error(err);
    Error.captureStackTrace(throwableError, $CONTEXT_PARAM.error);
    throw throwableError;
  }
}
