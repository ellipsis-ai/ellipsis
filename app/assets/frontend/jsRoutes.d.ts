interface JsRoute {
  url: string,
  method: "get" | "post",
  absoluteURL: (https: true) => string
}

declare var jsRoutes: {
  controllers: {
    APIAccessController: {
      linkCustomOAuth2Service: (configId: string, code?: Option<string>, state?: Option<string>, invocationId?: Option<string>, redirect?: Option<string>) => JsRoute
    },
    APITokenController: {
      listTokens: (tokenId: Option<string>, teamId?: Option<string>) => JsRoute
    },
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
      newUnsavedBehavior: (isDataType: boolean, isTest: boolean, teamId: string, behaviorIdToClone: string, newName: Option<string>) => JsRoute,
      newUnsavedLibrary: (teamId: string, libraryIdToClone: string) => JsRoute,
      nodeModuleVersionsFor: (groupId: string) => JsRoute,
      testResults: (groupId: string) => JsRoute,
      pushToGithub: () => JsRoute,
      queryDefaultStorage: () => JsRoute,
      save: () => JsRoute,
      saveDefaultStorageItem: () => JsRoute,
      testInvocation: () => JsRoute,
      testTriggers: () => JsRoute,
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
      save: () => JsRoute,
      validateRecurrence: () => JsRoute
    },
    SocialAuthController: {
      authenticateGithub: (redirectUrl?: Option<string>, teamId?: Option<string>, channelId?: Option<string>) => JsRoute
    },
    SupportController: {
      sendRequest: () => JsRoute
    },

    web: {
      settings: {
        EnvironmentVariablesController: {
          list: (teamId?: Option<string>) => JsRoute,
          submit: () => JsRoute,
          delete: () => JsRoute,
          adminLoadValue: (teamId: string, name: string) => JsRoute
        },
        IntegrationsController: {
          list: (teamId?: Option<string>) => JsRoute
        }
        RegionalSettingsController: {
          index: (teamId?: Option<string>) => JsRoute
        }
      }
    }

    admin: {
      UserInfoController: {
        userDataFor: (userId: string) => JsRoute
      }
    }
  }
};
