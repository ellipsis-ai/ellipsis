interface JsRoute {
  url: string,
  method: string
}

declare var jsRoutes: {
  controllers: {
    ApplicationController: {
      deleteBehaviorGroups: () => JsRoute,
      findBehaviorGroupsMatching: (queryString: string, branch: Option<string>, teamId: string) => JsRoute,
      index: (teamId?: string) => JsRoute,
      possibleCitiesFor: (search: string) => JsRoute,
      setTeamTimeZone: () => JsRoute,
      fetchPublishedBehaviorInfo: (teamId: string, branchName?: Option<string>) => JsRoute,
      mergeBehaviorGroups: () => JsRoute
    },
    BehaviorEditorController: {
      deleteDefaultStorageItems: () => JsRoute,
      deploy: () => JsRoute,
      edit: (groupId: string, selectedId?: string, showVersions?: boolean) => JsRoute,
      newGroup: (teamId: string) => JsRoute,
      newUnsavedBehavior: (isDataType: boolean, teamId: string, behaviorIdToClone: string, newName: Option<string>) => JsRoute,
      newUnsavedLibrary: (teamId: string, libraryIdToClone: string) => JsRoute,
      nodeModuleVersionsFor: (groupId: string) => JsRoute,
      pushToGithub: () => JsRoute,
      queryDefaultStorage: () => JsRoute,
      save: () => JsRoute,
      saveDefaultStorageItem: () => JsRoute,
      updateFromGithub: () => JsRoute,
      updateNodeModules: () => JsRoute,
      versionInfoFor: (groupId: string) => JsRoute
    },
    BehaviorImportExportController: {
      doImport: () => JsRoute,
      "export": (string) => JsRoute
    },
    GithubConfigController: {
      index: (teamId?: Option<string>) => JsRoute,
      reset: () => JsRoute
    },
    SavedAnswerController: {
      resetForTeam: () => JsRoute,
      resetForUser: () => JsRoute
    },
    ScheduledActionsController: {
      delete: () => JsRoute,
      index: (selectedId?: Option<string>, isNewSchedule?: Option<boolean>, teamId?: Option<string>, forceAdmin?: Option<boolean>) => JsRoute,
      save: () => JsRoute
    },
    SocialAuthController: {
      authenticateGithub: (redirectUrl?: Option<string>, teamId?: Option<string>, channelId?: Option<string>) => JsRoute
    },
    SupportController: {
      sendRequest: () => JsRoute
    },

    admin: {
      UserInfoController: {
        userDataFor: (userId: string) => JsRoute
      }
    }
  }
};
