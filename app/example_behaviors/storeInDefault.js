function(key, value, onSuccess, onError) {

    var request = require('request');

    request.post(
        "https://05f7c2f1.ngrok.io/put_item",
        { itemId: key, itemType: "stuff", teamId: Ellipsis.teamId, item: value },
        function (error, response, body) {
            if (!error && response.statusCode == 200) {
                onSuccess("Ok, got it!");
            } else {
                onError(error);
            }
        }
    );

}
