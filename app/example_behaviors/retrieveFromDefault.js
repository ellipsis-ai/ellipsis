function(key, onSuccess, onError) {

    var successFn = function(response, body) { onSuccess(body) };
    Ellipsis.db.getItem(key, "stuff", successFn, onError);

}
