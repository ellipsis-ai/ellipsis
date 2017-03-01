/* global BehaviorEditorConfiguration:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './behavior_editor/index', './models/behavior_group'],
    function(Core, Fetch, React, ReactDOM, BehaviorEditor, BehaviorGroup) {
      var config = Object.assign({}, BehaviorEditorConfiguration, {
        groupData: BehaviorEditorConfiguration.group,
        group: BehaviorGroup.fromJson(BehaviorEditorConfiguration.group),
        onSave: onSaveBehavior,
        onForgetSavedAnswerForInput: resetSavedAnswerForInput
      });

      var currentProps = config;

      function onSaveBehavior(newBehaviorData) {

        var props = Object.assign({}, currentProps, {
          group: currentProps.group.withNewBehaviorData(newBehaviorData),
          selectedBehaviorId: newBehaviorData.behaviorId,
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

      function fallbackSelectedBehaviorIdFor(group) {
        if (group.behaviorVersions.find(ea => ea.behaviorId === null)) {
          return null;
        } else {
          return group.behaviorVersions[0].behaviorId;
        }
      }

      const group = BehaviorGroup.fromJson(config.groupData);
      reload(Object.assign({}, config, {
        group: group,
        selectedBehaviorId: config.selectedBehaviorId ? config.selectedBehaviorId : fallbackSelectedBehaviorIdFor(group),
        justSaved: false
      }));
    }
  );
});
