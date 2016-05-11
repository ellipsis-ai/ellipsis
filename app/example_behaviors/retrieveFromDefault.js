function(key, onSuccess, onError) {

    var request = require('request');

    request.
        get(
            Ellipsis.db.getItemUrl,
            { itemId: key, itemType: Ellipsis.db.itemsTable, teamId: Ellipsis.teamId },
            function (error, response, body) {
                if (!error) {
                    onSuccess(body);
                } else {
                    onError(error);
                }
            }
        );

};
