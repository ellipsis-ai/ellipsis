const request = require('request');

const errorMessages = {
  ELLIPSIS_OBJECT_MISSING: "You need to pass an `ellipsis` object through from an Ellipsis action",
  MESSAGE_MISSING: "You need to pass a `message` argument",
  SCHEDULE_ACTION_MISSING: "You need to pass an `action` argument for the thing you want to schedule",
  UNSCHEDULE_ACTION_MISSING: "You need to pass an `action` argument for the thing you want to unschedule",
  RECURRENCE_MISSING: "You need to pass a `recurrence` argument to specify when you want to schedule the action to recur, e.g. \"every weekday at 9am\""
};

function errorHandler(ellipsis, args, message) {
  if (args && args.error) {
    args.error(message);
  } else if (ellipsis && ellipsis.error) {
    ellipsis.error(message);
  } else {
    throw message;
  }
}

const PM = {

  postMessage: function (args) {
    const ellipsis = args.ellipsis;
    if (typeof ellipsis !== "object") {
      errorHandler(null, args, errorMessages.ELLIPSIS_OBJECT_MISSING);
    } else {
      const message = args.message;
      if (!message) {
        errorHandler(ellipsis, args, errorMessages.MESSAGE_MISSING);
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
          }, (error, response, body) => {
            if (error) {
              errorHandler(ellipsis, args, error);
            } else if (response.statusCode !== 200) {
              errorHandler(ellipsis, args, response.statusCode + ": " + response.body);
            } else if (args.success) {
              args.success(response, body);
            } else {
              // do nothing if no success parameter was provided
            }
          }
        );
      }
    }
  },

  promiseToPostMessage: function(args) {
    const ellipsis = args.ellipsis;
    const message = args.message;
    if (typeof ellipsis !== "object") {
      errorHandler(null, args, errorMessages.ELLIPSIS_OBJECT_MISSING);
    } else if (!message) {
      errorHandler(ellipsis, args, errorMessages.MESSAGE_MISSING);
    } else {
      return new Promise((resolve, reject) => {
        PM.postMessage(Object.assign({}, args, {
          ellipsis: ellipsis,
          message: message,
          success: resolve,
          error: reject
        }));
      });
    }
  },

  promiseToSchedule: function(args) {
    const ellipsis = args.ellipsis;
    const action = args.action;
    const recurrence = args.recurrence;
    if (!action) {
      errorHandler(ellipsis, args, errorMessages.SCHEDULE_ACTION_MISSING);
    } else if (!recurrence) {
      errorHandler(ellipsis, args, errorMessages.RECURRENCE_MISSING);
    } else {
      const useDM = args.useDM ? "privately for everyone in this channel" : "";
      const message = `...schedule "${action}" ${useDM} ${recurrence}`;
      return PM.promiseToPostMessage(Object.assign({}, args, {
        message: message
      }));
    }
  },

  promiseToUnschedule: function(args) {
    const ellipsis = args.ellipsis;
    const action = args.action;
    if (!action) {
      errorHandler(ellipsis, args, errorMessages.UNSCHEDULE_ACTION_MISSING);
    } else {
      const message = `...unschedule "${action}"`;
      return PM.promiseToPostMessage(Object.assign({}, args, {
        message: message
      }));
    }
  }

};

module.exports = PM;
