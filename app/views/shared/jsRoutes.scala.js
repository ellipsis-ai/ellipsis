@()(implicit r: RequestHeader)
@import play.api.routing.JavaScriptReverseRouter
@JavaScriptReverseRouter("jsRoutes")(
  routes.javascript.APIAccessController.linkCustomOAuth2Service,
  routes.javascript.APIController.postMessage,
  routes.javascript.APIController.say,
  routes.javascript.APITokenController.createToken,
  routes.javascript.APITokenController.listTokens,
  routes.javascript.APITokenController.revokeToken,
  routes.javascript.ApplicationController.deleteBehaviorGroups,
  routes.javascript.ApplicationController.fetchPublishedBehaviorInfo,
  routes.javascript.ApplicationController.findBehaviorGroupsMatching,
  routes.javascript.ApplicationController.mergeBehaviorGroups,
  routes.javascript.ApplicationController.possibleCitiesFor,
  routes.javascript.ApplicationController.setTeamTimeZone,
  routes.javascript.BehaviorEditorController.deleteDefaultStorageItems,
  routes.javascript.BehaviorEditorController.edit,
  routes.javascript.BehaviorEditorController.newGroup,
  routes.javascript.BehaviorEditorController.newUnsavedBehavior,
  routes.javascript.BehaviorEditorController.newUnsavedLibrary,
  routes.javascript.BehaviorEditorController.queryDefaultStorage,
  routes.javascript.BehaviorEditorController.regexValidationErrorsFor,
  routes.javascript.BehaviorEditorController.save,
  routes.javascript.BehaviorEditorController.updateNodeModules,
  routes.javascript.BehaviorEditorController.saveDefaultStorageItem,
  routes.javascript.BehaviorEditorController.testInvocation,
  routes.javascript.BehaviorEditorController.testTriggers,
  routes.javascript.BehaviorEditorController.versionInfoFor,
  routes.javascript.BehaviorEditorController.nodeModuleVersionsFor,
  routes.javascript.BehaviorImportExportController.doImport,
  routes.javascript.BehaviorImportExportController.export,
  routes.javascript.EnvironmentVariablesController.list,
  routes.javascript.EnvironmentVariablesController.submit,
  routes.javascript.EnvironmentVariablesController.submit,
  routes.javascript.OAuth2ApplicationController.edit,
  routes.javascript.OAuth2ApplicationController.list,
  routes.javascript.OAuth2ApplicationController.newApp,
  routes.javascript.OAuth2ApplicationController.newApp,
  routes.javascript.OAuth2ApplicationController.save,
  routes.javascript.SavedAnswerController.resetForTeam,
  routes.javascript.SavedAnswerController.resetForUser,
  routes.javascript.ScheduledActionsController.index,
  routes.javascript.ScheduledActionsController.delete,
  routes.javascript.ScheduledActionsController.save
)
