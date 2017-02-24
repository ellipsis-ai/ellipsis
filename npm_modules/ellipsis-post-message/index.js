'use strict';

const request = require('request');

const errorMessages = {
  ELLIPSIS_OBJECT_MISSING: "You need to pass an `ellipsis` object through from an Ellipsis action",
  MESSAGE_MISSING: "You need to pass a `message` argument",
  ACTION_NAME_MISSING: "You need to pass an `actionName` argument",
  MESSAGE_AND_ACTION_NAME_MISSING: "You need to pass either an `actionName` or a `message` argument",
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

function handleResponse(args, ellipsis, error, response, body) {
  if (error) {
    errorHandler(ellipsis, args, error);
  } else if (response.statusCode !== 200) {
    errorHandler(ellipsis, args, response.statusCode + ": " + response.body);
  } else if (args.success) {
    args.success(response, body);
  } else {
    // do nothing if no success argument was provided
  }
}

function argsFormDataFor(args) {
  if (args) {
    let data = {};
    args.forEach((ea, i) => {
      data[`arguments[${i}].name`] = ea.name;
      data[`arguments[${i}].value`] = ea.value;
    });
    return data;
  } else {
    return {};
  }
}

const PM = {

  runAction: function (args) {
    const ellipsis = args.ellipsis;
    if (typeof ellipsis !== "object") {
      errorHandler(null, args, errorMessages.ELLIPSIS_OBJECT_MISSING);
    } else {
      const actionName = args.actionName;
      if (!actionName) {
        errorHandler(ellipsis, args, errorMessages.ACTION_NAME_MISSING);
      } else {
        const responseContext = args.responseContext ? args.responseContext : ellipsis.userInfo.messageInfo.medium;
        const channel = args.channel ? args.channel : ellipsis.userInfo.messageInfo.channel;
        const formData = Object.assign({
          actionName: actionName,
          responseContext: responseContext,
          channel: channel,
          token: ellipsis.token
        }, argsFormDataFor(args.args));
        request.
          post(
            {
              url: ellipsis.apiBaseUrl + "/api/run_action",
              form: formData
            }, (error, response, body) => handleResponse(args, ellipsis, error, response, body)
        );
      }
    }
  },

  promiseToRunAction: function(args) {
    const ellipsis = args.ellipsis;
    const actionName = args.actionName;
    if (typeof ellipsis !== "object") {
      errorHandler(null, args, errorMessages.ELLIPSIS_OBJECT_MISSING);
    } else if (!actionName) {
      errorHandler(ellipsis, args, errorMessages.ACTION_NAME_MISSING);
    } else {
      return new Promise((resolve, reject) => {
        PM.runAction(Object.assign({}, args, {
          ellipsis: ellipsis,
          actionName: actionName,
          success: resolve,
          error: reject
        }));
      });
    }
  },

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
          post(
            {
              url: ellipsis.apiBaseUrl + "/api/post_message",
              form: {
                message: message,
                responseContext: responseContext,
                channel: channel,
                token: ellipsis.token
              }
            }, (error, response, body) => handleResponse(args, ellipsis, error, response, body)
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

  say: function (args) {
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
          post(
            {
              url: ellipsis.apiBaseUrl + "/api/say",
              form: {
                message: message,
                responseContext: responseContext,
                channel: channel,
                token: ellipsis.token
              }
            }, (error, response, body) => handleResponse(args, ellipsis, error, response, body)
        );
      }
    }
  },

  promiseToSay: function(args) {
    const ellipsis = args.ellipsis;
    const message = args.message;
    if (typeof ellipsis !== "object") {
      errorHandler(null, args, errorMessages.ELLIPSIS_OBJECT_MISSING);
    } else if (!message) {
      errorHandler(ellipsis, args, errorMessages.MESSAGE_MISSING);
    } else {
      return new Promise((resolve, reject) => {
        PM.say(Object.assign({}, args, {
          ellipsis: ellipsis,
          message: message,
          success: resolve,
          error: reject
        }));
      });
    }
  },

  scheduleAction: function (args) {
    const ellipsis = args.ellipsis;
    if (typeof ellipsis !== "object") {
      errorHandler(null, args, errorMessages.ELLIPSIS_OBJECT_MISSING);
    } else {
      const actionName = args.actionName;
      if (!actionName) {
        errorHandler(ellipsis, args, errorMessages.ACTION_NAME_MISSING);
      } else {
        const responseContext = args.responseContext ? args.responseContext : ellipsis.userInfo.messageInfo.medium;
        const channel = args.channel ? args.channel : ellipsis.userInfo.messageInfo.channel;
        const formData = Object.assign({
          actionName: actionName,
          responseContext: responseContext,
          channel: channel,
          recurrence: args.recurrence,
          token: ellipsis.token
        }, argsFormDataFor(args.args));
        request.
          post(
            {
              url: ellipsis.apiBaseUrl + "/api/schedule_action",
              form: formData
            }, (error, response, body) => handleResponse(args, ellipsis, error, response, body)
        );
      }
    }
  },

  promiseToSchedule: function(args) {
    const ellipsis = args.ellipsis;
    const actionName = args.actionName;
    const message = args.message;
    const recurrence = args.recurrence;
    if (!actionName && !message) {
      errorHandler(ellipsis, args, errorMessages.MESSAGE_AND_ACTION_NAME_MISSING);
    } else if (!recurrence) {
      errorHandler(ellipsis, args, errorMessages.RECURRENCE_MISSING);
    } else {
      if (actionName) {
        return new Promise((resolve, reject) => {
          PM.scheduleAction(Object.assign({}, args, {
            success: resolve,
            error: reject
          }));
        });
      } else {
        const useDM = args.useDM ? "privately for everyone in this channel" : "";
        const fullMessage = `...schedule "${message}" ${useDM} ${recurrence}`;
        return PM.promiseToPostMessage(Object.assign({}, args, {
          message: fullMessage
        }));
      }
    }
  },

  unscheduleAction: function (args) {
    const ellipsis = args.ellipsis;
    if (typeof ellipsis !== "object") {
      errorHandler(null, args, errorMessages.ELLIPSIS_OBJECT_MISSING);
    } else {
      const actionName = args.actionName;
      if (!actionName) {
        errorHandler(ellipsis, args, errorMessages.ACTION_NAME_MISSING);
      } else {
        const formData = {
          actionName: actionName,
          token: ellipsis.token
        };
        request.
          post(
            {
              url: ellipsis.apiBaseUrl + "/api/unschedule_action",
              form: formData
            }, (error, response, body) => handleResponse(args, ellipsis, error, response, body)
        );
      }
    }
  },

  promiseToUnschedule: function(args) {
    const ellipsis = args.ellipsis;
    const actionName = args.actionName;
    const message = args.message;
    if (!actionName && !message) {
      errorHandler(ellipsis, args, errorMessages.MESSAGE_AND_ACTION_NAME_MISSING);
    } else {
      if (actionName) {
        return new Promise((resolve, reject) => {
          PM.unscheduleAction(Object.assign({}, args, {
            success: resolve,
            error: reject
          }));
        });
      } else {
        const fullMessage = `...unschedule "${message}"`;
        return PM.promiseToPostMessage(Object.assign({}, args, {
          message: fullMessage
        }));
      }
    }
  }

};

module.exports = PM;
