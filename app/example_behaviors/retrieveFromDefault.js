function(key, onSuccess, onError) {

    var request = require('request');

    request.
        get(
            Ellipsis.db.getItemUrl,
            { itemId: key, itemType: Ellipsis.db.itemsTable, teamId: Ellipsis.teamId },
            function (error, response, body) {
                if (!error && response.statusCode == 200) {
                    onSuccess(body);
                } else {
                    onError(error);
                }
            }
        );

};
