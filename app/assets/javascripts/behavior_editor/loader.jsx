/* global BehaviorEditorConfiguration:false */

requirejs(['../common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './lib/browser_utils', './behavior_editor/index', './models/behavior_group'],
    function(Core, Fetch, React, ReactDOM, BrowserUtils, BehaviorEditor, BehaviorGroup) {
      var config = Object.assign({}, BehaviorEditorConfiguration, {
        groupData: BehaviorEditorConfiguration.group,
        group: BehaviorGroup.fromJson(BehaviorEditorConfiguration.group),
        onSave: onSaveBehavior,
        onForgetSavedAnswerForInput: resetSavedAnswerForInput
      });

      var currentProps = config;

      function onSaveBehavior(newGroupData, state) {

        var props = Object.assign({}, currentProps, {
          group: BehaviorGroup.fromJson(newGroupData),
          justSaved: true
        });
        if (state) {
          props.selectedBehaviorId = state.selectedBehaviorId;
          props.group.behaviorVersions = props.group.behaviorVersions.map(ea => {
            const versionState = state.group.behaviorVersions.find(v => v.behaviorId === ea.behaviorId);
            if (versionState) {
              return ea.clone({ shouldRevealCodeEditor: versionState.shouldRevealCodeEditor });
            } else {
              return ea;
            }
          });
        }
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
      const selectedBehaviorId = config.selectedBehaviorId ? config.selectedBehaviorId : fallbackSelectedBehaviorIdFor(group);
      BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(group.id, selectedBehaviorId).url);
      reload(Object.assign({}, config, {
        group: group,
        selectedBehaviorId: selectedBehaviorId,
        justSaved: false
      }));
    }
  );
});
