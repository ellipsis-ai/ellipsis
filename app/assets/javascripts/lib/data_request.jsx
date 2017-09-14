define(function(require) {
  require('whatwg-fetch');

  class ResponseError extends Error {
    constructor(status, statusText) {
      super(`${status} ${statusText}`);
      this.status = status;
      this.statusText = statusText;
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
          throw new ResponseError(response.status, response.statusText);
        }
      });
    },

    jsonPost: function(url, body, csrfToken) {
      return fetch(url, {
        credentials: 'same-origin',
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Csrf-Token': csrfToken,
          'x-requested-with': 'XMLHttpRequest'
        },
        body: JSON.stringify(body)
      }).then((response) => {
        if (response.ok) {
          return response.json();
        } else {
          throw new ResponseError(response.status, response.statusText);
        }
      });
    }
  };
});
