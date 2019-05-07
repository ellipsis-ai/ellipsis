const request = require('request');
const FormData = require('form-data');

function addUploadFunctionsTo($CONTEXT_PARAM) {
  $CONTEXT_PARAM.uploadFromStream = uploadFromStream;
  $CONTEXT_PARAM.uploadFromUrl = uploadFromUrl;
  $CONTEXT_PARAM.uploadFromText = uploadFromText;

  return;

  function upload(uploadUrl, stream, textContent, filetype, filename) {
    return new Promise((resolve, reject) => {
      const form = new FormData();
      form.append('token', $CONTEXT_PARAM.token);
      if (filetype) {
        form.append('filetype', filetype);
      }
      if (filename) {
        form.append('filename', filename);
      }
      if (stream) {
        form.append('file', stream);
      } else if (textContent) {
        form.append('text', textContent);
      } else {
        throw new $CONTEXT_PARAM.Error("Either stream or text content must be provided");
      }
      form.submit(uploadUrl, function(err, res) {
        res.resume();
        res.on('data', (buffer) => {
          resolve(buffer.toString());
        });
      });
    });
  }

  function uploadFromText(text, filetype, filename) {
    const uploadUrl = `${$CONTEXT_PARAM.apiBaseUrl}/api/v1/upload_file_content`;
    return upload(uploadUrl, null, text, filetype, filename);
  }

  function uploadFromStream(stream, filetype, filename) {
    return upload(`${$CONTEXT_PARAM.apiBaseUrl}/api/v1/upload_file`, stream, null, filetype, filename);
  }

  function uploadFromUrl(url, filetype, filename) {
    return uploadFromStream(request(url), filetype, filename);
  }

};
