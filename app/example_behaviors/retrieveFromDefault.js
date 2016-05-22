function(key, onSuccess, onError, ellipsis) {

    var db = require('ellipsis-default-storage');

    db.getItem({
        itemId: key,
        itemType: "stuff",
        ellipsis: ellipsis,
        onSuccess: function(response, body) { onSuccess(body) },
        onError: onError
    });

}
