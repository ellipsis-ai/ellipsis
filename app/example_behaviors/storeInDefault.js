function(key, value, onSuccess, onError) {

    var successFn = function() { onSuccess("Ok, got it!") };
    Ellipsis.db.putItem(key, "stuff", value, successFn, onError);

}
