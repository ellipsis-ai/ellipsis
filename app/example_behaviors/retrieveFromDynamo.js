function(key, onSuccess, onError) {

    var doc = require('dynamodb-doc');
    var dynamo = new doc.DynamoDB();

    var payload = {
        "TableName": "stuff",
        "Key": {
            "key": key
        }
    };

    dynamo.getItem(payload, function(err, result) {
        if (err === null) {
            onSuccess(result.Item.value);
        } else {
            onError(err);
        }
    });
};
