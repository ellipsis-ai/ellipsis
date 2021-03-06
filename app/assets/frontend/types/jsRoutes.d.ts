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
      createToken: () => JsRoute,
      listTokens: (tokenId: Option<string>, teamId?: Option<string>) => JsRoute,
      revokeToken: () => JsRoute
    },
    ApplicationController: {
      deleteBehaviorGroups: () => JsRoute,
      findBehaviorGroupsMatching: (queryString: string, branch: Option<string>, teamId: string) => JsRoute,
      index: (teamId?: Option<string>) => JsRoute,
      possibleCitiesFor: (search: string) => JsRoute,
      getTimeZoneInfo: (timeZoneId: string) => JsRoute,
      setTeamTimeZone: () => JsRoute,
      fetchPublishedBehaviorInfo: (teamId: string, branchName?: Option<string>) => JsRoute,
      mergeBehaviorGroups: () => JsRoute
    },
    BehaviorEditorController: {
      deleteDefaultStorageItems: () => JsRoute,
      deploy: () => JsRoute,
      edit: (groupId: string, selectedId?: Option<string>, showVersions?: boolean) => JsRoute,
      linkToGithubRepo: () => JsRoute,
      metaData: (groupId: string) => JsRoute,
      newFromGithub: () => JsRoute,
      newGroup: (teamId: string, selectedId?: Option<string>) => JsRoute,
      groupWithNewUnsavedBehavior: () => JsRoute,
      newUnsavedLibrary: (teamId: string, libraryIdToClone: Option<string>) => JsRoute,
      nodeModuleVersionsFor: (groupId: string) => JsRoute,
      testResults: (groupId: string) => JsRoute,
      pushToGithub: () => JsRoute,
      queryDefaultStorage: () => JsRoute,
      regexValidationErrorsFor: (text: string) => JsRoute,
      save: () => JsRoute,
      saveDefaultStorageItem: () => JsRoute,
      testInvocation: () => JsRoute,
      testTriggers: () => JsRoute,
      updateFromGithub: () => JsRoute,
      updateNodeModules: () => JsRoute,
      versionInfoFor: (groupId: string) => JsRoute
    },
    CopilotController: {
      resultsSince: (listenerId: string, when: Option<string>) => JsRoute,
      sendToChannel: (invocationId: String) => JsRoute
    },
    BehaviorGroupConfigController: {
      schedules: (groupId: string) => JsRoute
    },
    BehaviorImportExportController: {
      doImport: () => JsRoute,
      "export": (id: string) => JsRoute
    },
    DashboardController: {
      skillManifest: (teamId?: Option<string>) => JsRoute,
      usage: (teamId?: Option<string>) => JsRoute
    },
    FeedbackController: {
      send: () => JsRoute
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
      index: (
        selectedId: Option<string>,
        isNewSchedule: Option<boolean>,
        filterChannelId: Option<string>,
        filterSkillId: Option<string>,
        teamId: Option<string>,
        forceAdmin: Option<boolean>
      ) => JsRoute,
      save: () => JsRoute,
      validateRecurrence: () => JsRoute,
      validateTriggers: () => JsRoute
    },
    SocialAuthController: {
      authenticateGithub: (redirectUrl?: Option<string>, teamId?: Option<string>, channelId?: Option<string>) => JsRoute
    },
    SupportController: {
      sendRequest: () => JsRoute
    },

    web: {
      settings: {
        AWSConfigController: {
          add: (teamId: Option<string>, groupId: Option<string>, selectedId: Option<string>, nameInCode: Option<string>) => JsRoute,
          edit: (id: string, teamId: Option<string>) => JsRoute,
          save: () => JsRoute
        },
        EnvironmentVariablesController: {
          list: (teamId?: Option<string>) => JsRoute,
          submit: () => JsRoute,
          delete: () => JsRoute,
          adminLoadValue: (teamId: string, name: string) => JsRoute
        },
        IntegrationsController: {
          add: (teamId: Option<string>, groupId: Option<string>, selectedId: Option<string>, nameInCode: Option<string>) => JsRoute,
          delete: () => JsRoute,
          edit: (id: string, teamId: Option<string>) => JsRoute,
          list: (teamId?: Option<string>) => JsRoute,
          save: () => JsRoute,
          resetSharedOAuthToken: (id: string, teamId: Option<string>) => JsRoute
        },
        RegionalSettingsController: {
          index: (teamId?: Option<string>) => JsRoute
        }
      }
    },

    api: {
      APIController: APIController
    },

    admin: {
      UserInfoController: {
        userDataFor: (userId: string) => JsRoute
      }
    }
  }
};

declare interface APIController {
  postMessage(): JsRoute
  say(): JsRoute
}
