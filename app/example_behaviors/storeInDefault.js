function(key, value, onSuccess, onError, context) {

    var db = require("ellipsis-default-storage")(context);

    db.putItem(key, "stuff", value, function() { onSuccess("Ok, got it!") }, onError);

}
