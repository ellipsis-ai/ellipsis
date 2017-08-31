function ellipsisSuccessCallback(result, options) {
  var resultWithOptions = Object.assign({}, options, {
    result: result === undefined ? null : result
  });
  callback(null, resultWithOptions);
}
