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
            onError("Missing values for: " + missing.join(", "));
        } else {
            request.
                post({
                    url: args.ellipsis.apiBaseUrl + "/put_item",
                    form: {itemId: args.itemId, itemType: args.itemType, token: args.ellipsis.token, item: args.item}
                }, function (error, response, body) {
                    if (!error && response.statusCode == 200) {
                        if (args.onSuccess) {
                            args.onSuccess(response, body);
                        }
                    } else {
                        if (args.onError) {
                            args.onError(error);
                        }
                    }
                }
            );
        }
    },

    getItem: function (args) {
        var missing = findMissingArgs(["itemId", "itemType", "ellipsis"], args);
        if (missing.length > 0) {
            onError("Missing values for: " + missing.join(", "));
        } else {
            request.
                get(
                args.context.apiBaseUrl + "/get_item/" + args.itemId + "/" + args.itemType + "/" + args.ellipsis.token,
                function (error, response, body) {
                    if (!error && response.statusCode == 200) {
                        if (args.onSuccess) {
                            args.onSuccess(response, body);
                        }
                    } else {
                        if (args.onError) {
                            args.onError(error);
                        }
                    }
                }
            );
        }
    }
};
