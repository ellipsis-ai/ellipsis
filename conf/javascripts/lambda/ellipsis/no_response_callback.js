function ellipsisNoResponseCallback() {
  process.removeListener('unhandledRejection', $CONTEXT_PARAM.error);
  callback(null, {
    $NO_RESPONSE_KEY: true
  });
}
