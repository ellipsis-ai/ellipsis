/* global log:false */
function ellipsisSuccessCallback(result, options) {
  var resultWithOptions = Object.assign({}, options, {
    result: result === undefined ? null : result,
    logs: log
  });
  process.removeListener('unhandledRejection', $CONTEXT_PARAM.error);
  callback(null, resultWithOptions);
}
