requirejs(['common'], function() {
  requirejs(
    ['core-js', 'whatwg-fetch', 'react', 'react-dom', './lib/browser_utils', './behavior_editor/index', './models/behavior_group', 'config/behavioreditor/edit', './models/param_type'],
    function(Core, Fetch, React, ReactDOM, BrowserUtils, BehaviorEditor, BehaviorGroup, BehaviorEditorConfiguration, ParamType) {
      var config = Object.assign({}, BehaviorEditorConfiguration, {
        groupData: BehaviorEditorConfiguration.group,
        group: BehaviorGroup.fromJson(BehaviorEditorConfiguration.group),
        builtinParamTypes: BehaviorEditorConfiguration.builtinParamTypes.map(ParamType.fromJson),
        onSave: onSave,
        onForgetSavedAnswerForInput: resetSavedAnswerForInput
      });

      var currentProps = config;

      function onSave(newProps, state) {
        const props = Object.assign({}, currentProps, newProps);
        if (state) {
          props.group = props.group.clone({
            behaviorVersions: props.group.behaviorVersions.map(ea => {
              const versionState = state.group.behaviorVersions.find(v => v.behaviorId === ea.behaviorId);
              if (versionState) {
                return ea.clone({ shouldRevealCodeEditor: versionState.shouldRevealCodeEditor });
              } else {
                return ea;
              }
            })
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

      function fallbackSelectedIdFor(group) {
        var isSimpleBehaviorGroup = !group.name && !group.description && group.behaviorVersions.length === 1;
        if (isSimpleBehaviorGroup) {
          return group.behaviorVersions[0].behaviorId;
        } else {
          return null;
        }
      }

      const group = BehaviorGroup.fromJson(config.groupData);
      const selectedId = config.selectedId || fallbackSelectedIdFor(group);
      if (group.id && selectedId) {
        BrowserUtils.replaceURL(jsRoutes.controllers.BehaviorEditorController.edit(group.id, selectedId).url);
      }
      reload(Object.assign({}, config, {
        group: group,
        selectedId: selectedId
      }));
    }
  );
});
