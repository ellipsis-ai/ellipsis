// @flow
type JsRoute = {
  url: string,
  method: string
}

declare var jsRoutes: {
  controllers: {
    ApplicationController: {
      deleteBehaviorGroups: () => JsRoute,
      findBehaviorGroupsMatching: (queryString: string, branch: ?string, teamId: string) => JsRoute,
      index: (teamId?: string) => JsRoute,
      setTeamTimeZone: () => JsRoute,
      fetchPublishedBehaviorInfo: (teamId: string, branchName?: ?string) => JsRoute,
      mergeBehaviorGroups: () => JsRoute
    },
    BehaviorEditorController: {
      deleteDefaultStorageItems: () => JsRoute,
      edit: (groupId: string, selectedId?: string, showVersions?: boolean) => JsRoute,
      pushToGithub: () => JsRoute,
      queryDefaultStorage: () => JsRoute,
      save: () => JsRoute,
      saveDefaultStorageItem: () => JsRoute,
      updateFromGithub: () => JsRoute
    },
    BehaviorImportExportController: {
      doImport: () => JsRoute,
      "export": (string) => JsRoute
    },
    GithubConfigController: {
      index: () => JsRoute,
      reset: () => JsRoute
    },
    SocialAuthController: {
      authenticateGithub: (string) => JsRoute
    }
  }
};
