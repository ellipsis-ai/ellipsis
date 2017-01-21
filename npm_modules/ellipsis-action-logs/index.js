const request = require('request');

module.exports = {

  get: function (args) {
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
      const actionTriggerOrId = args.action;
      if (!actionTriggerOrId) {
        errorHandler("You need to pass an `action` argument");
      } else {
        const url = `${ellipsis.apiBaseUrl}/get_action_logs/${actionTriggerOrId}/${ellipsis.token}`;
        const queryParams = {
          from: args.from.toISOString(),
          to: args.to.toISOString()
        };
        if (args.userId) {
          queryParams.userId = args.userId;
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
