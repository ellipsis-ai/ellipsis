const request = require('request');
const FormData = require('form-data');

function addUploadFunctionsTo($CONTEXT_PARAM) {
  $CONTEXT_PARAM.uploadFromStream = uploadFromStream;
  $CONTEXT_PARAM.uploadFromUrl = uploadFromUrl;
  $CONTEXT_PARAM.uploadFromText = uploadFromText;

  return;

  function upload(options) {
    return new Promise((resolve, reject) => {
      const form = new FormData();
      form.append('token', $CONTEXT_PARAM.token);
      if (options.filetype) {
        form.append('filetype', options.filetype);
      }
      if (options.filename) {
        form.append('filename', options.filename);
      }
      if (options.stream) {
        form.append('file', options.stream);
      } else if (options.text) {
        form.append('text', options.text);
      } else {
        throw new $CONTEXT_PARAM.Error("Either stream or text content must be provided");
      }
      form.submit(options.uploadUrl, function(err, res) {
        if (res.statusCode !== 200) {
          res.resume();
          res.on('data', (buffer) => {
            reject(`Unsuccessful response from the API: ${res.statusCode} ${res.statusMessage}: ${buffer.toString()}`);
          });
        } else {
          res.resume();
          res.on('data', (buffer) => {
            resolve(buffer.toString());
          });
          res.on('error', (err) => {
            reject(err);
          });
        }
      });
    });
  }

  function uploadFromText(text, options) {
    const mergedOptions = Object.assign({}, options, {
      uploadUrl: `${$CONTEXT_PARAM.apiBaseUrl}/api/v1/upload_file_content`,
      text: text
    });
    return upload(mergedOptions);
  }

  function uploadFromStream(stream, options) {
    const mergedOptions = Object.assign({}, options, {
      uploadUrl: `${$CONTEXT_PARAM.apiBaseUrl}/api/v1/upload_file`,
      stream: stream
    });
    return upload(mergedOptions);
  }

  function uploadFromUrl(url, options) {
    return uploadFromStream(request(url), options);
  }

};
