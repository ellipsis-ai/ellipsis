@()(implicit r: RequestHeader)
@import play.api.routing.JavaScriptReverseRouter

var requirejs = {
  paths: {
    '../common': '@RemoteAssets.getUrl("javascripts/common.js").replaceFirst("\\.js$", "")'
  }
};

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
  routes.javascript.BehaviorEditorController.edit,
  routes.javascript.BehaviorEditorController.newGroup,
  routes.javascript.BehaviorEditorController.newUnsavedBehavior,
  routes.javascript.BehaviorEditorController.newUnsavedInput,
  routes.javascript.BehaviorEditorController.regexValidationErrorsFor,
  routes.javascript.BehaviorEditorController.save,
  routes.javascript.BehaviorEditorController.testInvocation,
  routes.javascript.BehaviorEditorController.testTriggers,
  routes.javascript.BehaviorEditorController.versionInfoFor,
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
  routes.javascript.SavedAnswerController.resetForUser
)
