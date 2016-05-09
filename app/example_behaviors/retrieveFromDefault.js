function(key, onSuccess, onError) {

    var request = require('request');

    request.get(
        "https://05f7c2f1.ngrok.io/get_item",
        { itemId: key, itemType: "stuff", teamId: Ellipsis.teamId },
        function (error, response, body) {
            if (!error && response.statusCode == 200) {
                onSuccess(body);
            } else {
                onError(error);
            }
        }
    );

};
