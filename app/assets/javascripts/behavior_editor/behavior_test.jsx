define(function() {

  function testInvocationUrl() {
    return jsRoutes.controllers.BehaviorEditorController.testInvocation().url;
  }

  function testTriggersUrl() {
    return jsRoutes.controllers.BehaviorEditorController.testTriggers().url;
  }

  var commonRequiredParams = [
    'behaviorId',
    'csrfToken',
    'onSuccess',
    'onError'
  ];

  function checkParamsFor(params, requiredParams) {
    var missingParams = requiredParams.filter((paramName) => !params[paramName]);
    if (!params || missingParams.length > 0) {
      throw new Error("Required parameters missing: " + missingParams.join(", "));
    }
  }

  function request(url, params, formData) {
    formData.append('behaviorId', params.behaviorId);
    fetch(url, {
      credentials: 'same-origin',
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Csrf-Token': params.csrfToken
      },
      body: formData
    })
      .then((response) => response.json())
      .then(params.onSuccess)
      .catch(params.onError);
  }

  return {
    testInvocation: function(params) {
      checkParamsFor(params, commonRequiredParams.concat('paramValues'));
      var formData = new FormData();
      formData.append('paramValuesJson', JSON.stringify(params.paramValues));
      request(testInvocationUrl(), params, formData);
    },

    testTriggers: function(params) {
      checkParamsFor(params, commonRequiredParams.concat('message'));
      var formData = new FormData();
      formData.append('message', params.message);
      request(testTriggersUrl(), params, formData);
    }
  };
});
