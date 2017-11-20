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
  routes.javascript.BehaviorEditorController.metaData,
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
  routes.javascript.BehaviorEditorController.updateFromGithub,
  routes.javascript.BehaviorEditorController.pushToGithub,
  routes.javascript.BehaviorEditorController.linkToGithubRepo,
  routes.javascript.BehaviorImportExportController.doImport,
  routes.javascript.BehaviorImportExportController.export,
  routes.javascript.FeedbackController.send,
  routes.javascript.SavedAnswerController.resetForTeam,
  routes.javascript.SavedAnswerController.resetForUser,
  routes.javascript.ScheduledActionsController.index,
  routes.javascript.ScheduledActionsController.delete,
  routes.javascript.ScheduledActionsController.save,
  routes.javascript.SocialAuthController.authenticateGithub,

  // Settings
  routes.javascript.GithubConfigController.index,
  routes.javascript.GithubConfigController.reset,

  web.settings.routes.javascript.EnvironmentVariablesController.list,
  web.settings.routes.javascript.EnvironmentVariablesController.submit,
  web.settings.routes.javascript.RegionalSettingsController.index,
  web.settings.routes.javascript.IntegrationsController.list,
  web.settings.routes.javascript.IntegrationsController.add,
  web.settings.routes.javascript.AWSConfigController.add,
  web.settings.routes.javascript.AWSConfigController.edit,
  web.settings.routes.javascript.AWSConfigController.save,
  web.settings.routes.javascript.OAuth2ApplicationController.edit,
  web.settings.routes.javascript.OAuth2ApplicationController.add,
  web.settings.routes.javascript.OAuth2ApplicationController.save
)
