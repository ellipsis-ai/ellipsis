const request = require('request');
const FormData = require('form-data');

function addUploadFunctionsTo($CONTEXT_PARAM) {
  $CONTEXT_PARAM.uploadFromStream = uploadFromStream;
  $CONTEXT_PARAM.uploadFromUrl = uploadFromUrl;
  $CONTEXT_PARAM.uploadFromText = uploadFromText;

  return;

  function upload(stream, textContent) {
    const uploadUrl = `${$CONTEXT_PARAM.apiBaseUrl}/api/v1/upload_file`;
    return new Promise((resolve, reject) => {
      const form = new FormData();
      form.append('token', $CONTEXT_PARAM.token);
      if (stream) {
        form.append('file', stream);
      } else if (textContent) {
        form.append('text', stream);
      } else {
        throw new $CONTEXT_PARAM.Error("Either stream or text content must be provided");
      }
      form.submit(uploadUrl, function(err, res) {
        res.resume();
        res.on('data', (buffer) => {
          resolve(JSON.parse(buffer.toString()));
        });
      });
    });
  }

  function uploadFromText(text) {
    return upload(null, text);
  }

  function uploadFromStream(stream) {
    return upload(stream, null);
  }

  function uploadFromUrl(url) {
    return uploadFromStream(request(url));
  }

};
