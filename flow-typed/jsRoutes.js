// @flow
type JsRoute = {
  url: string
}

declare var jsRoutes: {
  controllers: {
    ApplicationController: {
      setTeamTimeZone: () => JsRoute
    },
    BehaviorEditorController: {
      edit: (string) => JsRoute
    },
    BehaviorImportExportController: {
      "export": (string) => JsRoute
    },
    SocialAuthController: {
      authenticateGithub: (string) => JsRoute
    }
  }
};
