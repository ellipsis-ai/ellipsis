import 'whatwg-fetch';

class ResponseError extends Error {
    status: number;
    statusText: string;
    body: Option<string>;

    constructor(status: number, statusText: string, body: Option<string>) {
      super(`${status} ${statusText}`);
      this.status = status;
      this.statusText = statusText;
      this.body = body;
    }
}

const DataRequest = {
    jsonGet: function(url: string): Promise<any> {
      return fetch(url, {
        credentials: 'same-origin'
      }).then((response: Response) => {
        if (response.ok) {
          return response.json();
        } else {
          return response.text().then((body: string) => {
            throw new ResponseError(response.status, response.statusText, body);
          });
        }
      });
    },

    jsonPost: function(url: string, requestBody: {}, csrfToken: string): Promise<any> {
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
      }).then((response: Response) => {
        if (response.ok) {
          return response.json();
        } else {
          return response.text().then((resultBody: string) => {
            throw new ResponseError(response.status, response.statusText, resultBody);
          });
        }
      });
    }
};

export {DataRequest, ResponseError};
