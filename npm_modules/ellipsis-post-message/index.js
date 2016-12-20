const request = require('request');

module.exports = {

  postMessage: function (args) {
    const ellipsis = args.ellipsis;
    if (typeof ellipsis !== "object") {
      const msg = "You need to pass an `ellipsis` object through from an Ellipsis action";
      if (args.error) {
        args.error(msg);
      } else {
        throw msg;
      }
    } else {
      const errorHandler = typeof args.error === "function" ? args.error : ellipsis.error;
      const message = args.message;
      if (!message) {
        errorHandler("You need to pass a `message` argument");
      } else {
        const responseContext = args.responseContext ? args.responseContext : ellipsis.userInfo.messageInfo.medium;
        const channel = args.channel ? args.channel : ellipsis.userInfo.messageInfo.channel;
        request.
          post({
            url: ellipsis.apiBaseUrl + "/api/post_message",
            form: {
              message: message,
              responseContext: responseContext,
              channel: channel,
              token: ellipsis.token
            }
          }, function (error, response, body) {
            if (error) {
              errorHandler(error)
            } else {
              if (response.statusCode != 200) {
                errorHandler(response.statusCode + ": " + response.body);
              } else {
                if (args.success) {
                  args.success(response, body);
                }
              }
            }
          }
        );
      }
    }
  }

};
