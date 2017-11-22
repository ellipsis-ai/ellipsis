// @flow
define(function(require) {
  require('whatwg-fetch');

  class ResponseError extends Error {
    status: number;
    statusText: string;
    body: ?string;

    constructor(status, statusText, body) {
      super(`${status} ${statusText}`);
      this.status = status;
      this.statusText = statusText;
      this.body = body;
    }
  }

  return {
    jsonGet: function(url) {
      return fetch(url, {
        credentials: 'same-origin'
      }).then((response) => {
        if (response.ok) {
          return response.json();
        } else {
          return response.text().then(body => {
            throw new ResponseError(response.status, response.statusText, body);
          });
        }
      });
    },

    jsonPost: function(url, requestBody, csrfToken) {
      return fetch(url, {
        credentials: 'same-origin',
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Csrf-Token': csrfToken,
          'x-requested-with': 'XMLHttpRequest'
        },
        body: JSON.stringify(requestBody)
      }).then((response) => {
        if (response.ok) {
          return response.json();
        } else {
          return response.text().then(resultBody => {
            throw new ResponseError(response.status, response.statusText, resultBody);
          });
        }
      });
    },

    ResponseError: ResponseError
  };
});
