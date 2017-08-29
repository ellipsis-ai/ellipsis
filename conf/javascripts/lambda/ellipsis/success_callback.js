function ellipsisSuccessCallback(result, options) {
  var resultWithOptions = Object.assign(
    {},
    { "result": result === undefined ? null : result },
    options ? options : {}
  );
  callback(null, resultWithOptions);
}
