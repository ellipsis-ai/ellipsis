function(key, onSuccess, onError, context) {

    var db = require('ellipsis-default-storage');

    db.getItem({
        itemId: key,
        itemType: "stuff",
        context: context,
        onSuccess: function(response, body) { onSuccess(body) },
        onError: onError
    });

}
