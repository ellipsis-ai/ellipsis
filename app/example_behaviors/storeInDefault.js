function(key, value, onSuccess, onError, context) {

    var db = require("ellipsis-default-storage");

    db.putItem(key, "stuff", value, function() { onSuccess("Ok, got it!") }, onError, context);

}
