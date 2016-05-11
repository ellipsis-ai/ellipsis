function(key, value, onSuccess, onError) {

    var request = require('request');

    request.
        post(
            Ellipsis.db.putItemUrl,
            { itemId: key, itemType: Ellipsis.db.itemsTable, teamId: Ellipsis.teamId, item: value },
            function (error, response, body) {
                if (!error) {
                    onSuccess("Ok, got it!");
                } else {
                    onError(error);
                }
            }
        );

}
