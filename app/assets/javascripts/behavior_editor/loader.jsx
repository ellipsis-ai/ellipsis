/* global BehaviorEditorConfiguration:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_editor/index', './models/behavior_version'],
    function(Core, Fetch, React, ReactDOM, BehaviorEditor, BehaviorVersion) {
      var config = Object.assign({}, BehaviorEditorConfiguration, {
        otherBehaviorsInGroup: BehaviorEditorConfiguration.otherBehaviorsInGroup.map((ea) => BehaviorVersion.fromJson(ea)),
        onSave: onSaveBehavior,
        onForgetSavedAnswerForInput: resetSavedAnswerForInput
      });

      var currentProps;

      function onSaveBehavior(newData) {
        var props = Object.assign({}, config, newData, {
          behavior: BehaviorVersion.fromJson(newData),
          justSaved: true
        });
        reload(props);
      }

      function reload(props) {
        var myBehaviorEditor = React.createElement(BehaviorEditor, props);
        ReactDOM.render(myBehaviorEditor, document.getElementById(config.containerId));
        currentProps = props;
      }

      function resetSavedAnswerForInput(inputId, numAnswersDeleted) {
        var newSavedAnswers = currentProps.savedAnswers.map((ea) => {
          if (ea.inputId === inputId) {
            return Object.assign({}, ea, {
              myValueString: null,
              userAnswerCount: ea.userAnswerCount - numAnswersDeleted
            });
          } else {
            return ea;
          }
        });
        reload(Object.assign({}, currentProps, {
          savedAnswers: newSavedAnswers
        }));
      }

      reload(Object.assign({}, config, config.data, {
        behavior: BehaviorVersion.fromJson(config.data),
        justSaved: false
      }));
    }
  );
});
