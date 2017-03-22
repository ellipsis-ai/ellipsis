define(function(require) {
  require('whatwg-fetch');

  return {
    jsonGet: function(url) {
      return fetch(url, {
        credentials: 'same-origin'
      }).then((response) => response.json());
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
      }).then((response) => response.json());
    }
  };
});
