const request = require('request');
const USER_ERROR_MESSAGE = "An unknown error occurred while trying to read the file you uploaded.";
module.exports = function(param, $CONTEXT_PARAM) {
  return function() {
    const url = $CONTEXT_PARAM.apiBaseUrl + "/api/v1/files";
    const qs = { token: $CONTEXT_PARAM.token, fileId: param.id };
    const commonOptions = {
      url: url,
      qs: qs
    };
    const headOptions = Object.assign({}, commonOptions, { method: "HEAD" });
    return new Promise((resolve, reject) => {
      request(headOptions, (err, res, body) => {
        if (res.statusCode === 200) {
          const contentType = res.headers["content-type"];
          const contentDisposition = res.headers["content-disposition"];
          const filenameMatch = contentDisposition ?
            contentDisposition.match(/;?\s*filename="(.+?)"/) :
            undefined;
          const filename = (filenameMatch && filenameMatch[1]) || 'ellipsis.txt';
          resolve({
            value: request(commonOptions),
            contentType: contentType,
            filename: filename
          });
        } else {
          const errorMessage = `${res.statusCode}: ${res.statusMessage}`;
          reject(new $CONTEXT_PARAM.Error(err || errorMessage, {
            userMessage: USER_ERROR_MESSAGE
          }));
        }
      });
    });
  };
};
