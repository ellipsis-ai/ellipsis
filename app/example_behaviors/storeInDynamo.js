function(key, value, onSuccess, onError) {

    var doc = require('dynamodb-doc');
    var dynamo = new doc.DynamoDB();

    var payload = {
        "TableName": "stuff",
        "Item": {
            "key": key,
            "value": value
        }
    };

    dynamo.putItem(payload, function(err, result) {
        if (err === null) {
            onSuccess("Ok, got it!");
        } else {
            onError(err);
        }
    });
}
