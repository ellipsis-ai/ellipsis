function(key, onSuccess, onError, context) {

    var db = require('ellipsis-default-storage');

    db.getItem(key, "stuff", function(response, body) { onSuccess(body) }, onError, context);

}
