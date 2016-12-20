const request = require('request');

function findMissingArgs(keysToEnsure, args) {
  const missing = [];
  keysToEnsure.forEach(function(key) {
    if (args[key] === undefined) {
      missing.push(key)
    }
  });
  return missing;
}

module.exports = {

  postMessage: function (args) {
    const missing = findMissingArgs(["message", "ellipsis"], args);
    if (missing.length > 0) {
      if (args.error) {
        args.error("Missing values for: " + missing.join(", "));
      }
    } else {
      const ellipsis = args.ellipsis;
      const responseContext = args.responseContext ? args.responseContext : ellipsis.userInfo.messageInfo.medium;
      const channel = args.channel ? args.channel : ellipsis.userInfo.messageInfo.channel;
      request.
        post({
          url: ellipsis.apiBaseUrl + "/api/post_message",
          form: {
            message: args.message,
            responseContext: responseContext,
            channel: channel,
            token: args.ellipsis.token
          }
        }, function (error, response, body) {
          if (error) {
            if (args.error) {
              args.error(error);
            }
          } else {
            if (response.statusCode != 200) {
              args.error(response.statusCode + ": " + response.body);
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

};
