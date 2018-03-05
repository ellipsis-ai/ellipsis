interface JsRoute {
  url: string,
  method: string
}

declare var jsRoutes: {
  controllers: {
    ApplicationController: {
      deleteBehaviorGroups: () => JsRoute,
      findBehaviorGroupsMatching: (queryString: string, branch: string | null, teamId: string) => JsRoute,
      index: (teamId?: string) => JsRoute,
      possibleCitiesFor: (search: string) => JsRoute,
      setTeamTimeZone: () => JsRoute,
      fetchPublishedBehaviorInfo: (teamId: string, branchName?: string | null) => JsRoute,
      mergeBehaviorGroups: () => JsRoute
    },
    BehaviorEditorController: {
      deleteDefaultStorageItems: () => JsRoute,
      deploy: () => JsRoute,
      edit: (groupId: string, selectedId?: string, showVersions?: boolean) => JsRoute,
      newGroup: (teamId: string) => JsRoute,
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
    ScheduledActionsController: {
      index: (selectedId?: string | null, isNewSchedule?: boolean | null, teamId?: string | null) => JsRoute
    },
    SocialAuthController: {
      authenticateGithub: (string) => JsRoute
    }
  }
};
