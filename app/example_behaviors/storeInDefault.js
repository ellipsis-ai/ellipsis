function(key, value, onSuccess, onError, context) {

    var db = require("ellipsis-default-storage");

    db.putItem({
        itemId: key,
        itemType: "stuff",
        item: value,
        context: context,
        onSuccess: function() { onSuccess("Ok, got it!") },
        onError: onError
    });

}
