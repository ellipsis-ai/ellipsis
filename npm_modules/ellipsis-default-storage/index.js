var request = require('request');

function findMissingArgs(keysToEnsure, args) {
    var missing = [];
    keysToEnsure.forEach(function(key) {
        if (args[key] === undefined) {
            missing.push(key)
        }
    });
    return missing;
}

module.exports = {
    putItem: function (args) {
        var missing = findMissingArgs(["itemId", "itemType", "item", "ellipsis"], args);
        if (missing.length > 0) {
          if (args.onError) {
            args.onError("Missing values for: " + missing.join(", "));
          }
        } else {
            request.
                post({
                    url: args.ellipsis.apiBaseUrl + "/put_item",
                    form: {
                      itemId: args.itemId,
                      itemType: args.itemType,
                      token: args.ellipsis.token,
                      item: args.item
                    }
                }, function (error, response, body) {
                    if (!error && response.statusCode == 200) {
                        if (args.onSuccess) {
                            args.onSuccess(response, body);
                        }
                    } else {
                        if (args.onError) {
                            args.onError(error, response.statusCode, body);
                        }
                    }
                }
            );
        }
    },

    getItem: function (args) {
        var missing = findMissingArgs(["itemId", "itemType", "ellipsis"], args);
        if (missing.length > 0) {
            if (args.onError) {
                args.onError("Missing values for: " + missing.join(", "));
            }
        } else {
            request.get(
                args.ellipsis.apiBaseUrl + "/get_item/" + encodeURIComponent(args.itemId) + "/" + encodeURIComponent(args.itemType) + "/" + encodeURIComponent(args.ellipsis.token),
                function (error, response, body) {
                    if (!error && response.statusCode == 200) {
                        if (args.onSuccess) {
                            args.onSuccess(response, body);
                        }
                    } else {
                        if (args.onError) {
                            args.onError(error, response.statusCode, body);;
                        }
                    }
                }
            );
        }
    }
};
