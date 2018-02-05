// @flow
type JsRoute = {
  url: string,
  method: string
}

declare var jsRoutes: {
  controllers: {
    ApplicationController: {
      setTeamTimeZone: () => JsRoute
    },
    BehaviorEditorController: {
      deleteDefaultStorageItems: () => JsRoute,
      edit: (groupId: string, selectedId?: string, showVersions?: boolean) => JsRoute,
      pushToGithub: () => JsRoute,
      queryDefaultStorage: () => JsRoute,
      saveDefaultStorageItem: () => JsRoute,
      updateFromGithub: () => JsRoute
    },
    BehaviorImportExportController: {
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
