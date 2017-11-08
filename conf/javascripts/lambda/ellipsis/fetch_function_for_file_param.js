module.exports = function(param, $CONTEXT_PARAM) {
  const request = require('request');
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
          const filename =
            contentDisposition ?
              contentDisposition.match(/\S+\s+filename="(.+?)"/)[1] :
              'ellipsis.txt';
          resolve({
            value: request(commonOptions),
            contentType: contentType,
            filename: filename
          });
        } else if (err) {
          reject(err);
        } else {
          reject(`${res.statusCode}: ${body}`);
        }
      });
    });
  };
};
