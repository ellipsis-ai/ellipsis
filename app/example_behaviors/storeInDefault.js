function(key, value, onSuccess, onError, ellipsis) {

    var db = require("ellipsis-default-storage");

    db.putItem({
        itemId: key,
        itemType: "stuff",
        item: value,
        ellipsis: ellipsis,
        onSuccess: function() { onSuccess("Ok, got it!") },
        onError: onError
    });

}
