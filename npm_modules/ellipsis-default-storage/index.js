var request = require('request');

module.exports = function(context) {

    return {

        putItem: function (itemId, itemType, item, onSuccess, onError) {
            request.
                post({
                    url: context.db.putItemUrl,
                    form: {itemId: itemId, itemType: itemType, teamId: context.teamId, item: item}
                }, function (error, response, body) {
                    if (!error && response.statusCode == 200) {
                        onSuccess(response, body);
                    } else {
                        onError(error);
                    }
                }
            );
        },

        getItem: function (itemId, itemType, onSuccess, onError) {
            request.
                get(
                context.db.getItemUrl + "/" + itemId + "/" + itemType + "/" + context.teamId,
                function (error, response, body) {
                    if (!error && response.statusCode == 200) {
                        onSuccess(response, body);
                    } else {
                        onError(error);
                    }
                }
            );
        }
    }
};
