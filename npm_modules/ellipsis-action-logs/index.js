const request = require('request');

module.exports = {

  get: function (args) {
    const ellipsis = args.ellipsis;
    if (typeof ellipsis !== "object") {
      const msg = "You need to pass an `ellipsis` object through from an Ellipsis action";
      if (args.validationError) {
        args.validationError(msg);
      } else {
        throw msg;
      }
    } else {
      const errorHandler = typeof args.validationError === "function" ? args.validationError : ellipsis.validationError;
      const actionName = args.action || args.actionName;
      if (!actionName) {
        errorHandler("You need to pass an `action` argument");
      } else {
        const url = `${ellipsis.apiBaseUrl}/get_action_logs/${actionName}/${ellipsis.token}`;
        const queryParams = {
          from: args.from.toISOString(),
          to: args.to.toISOString()
        };
        if (args.userId) {
          queryParams.userId = args.userId;
        }
        if (args.originalEventType) {
          queryParams.originalEventType = args.originalEventType;
        }
        request.
          get({
            url: url,
            qs: queryParams
          }, function (error, response) {
            if (error) {
              errorHandler(error);
            } else {
              if (response.statusCode !== 200) {
                errorHandler(response.statusCode + ": " + response.body);
              } else {
                if (args.success) {
                  args.success(JSON.parse(response.body));
                }
              }
            }
          }
        );
      }
    }
  }

};
