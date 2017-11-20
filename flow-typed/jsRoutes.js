declare var jsRoutes: {
  controllers: {
    ApplicationController: {
      setTeamTimeZone: () => {
        url: string
      }
    },
    BehaviorEditorController: {
      edit: (string) => {
        url: string
      }
    },
    BehaviorImportExportController: {
      "export": (string) => {
        url: string
      }
    },
    SocialAuthController: {
      authenticateGithub: (string) => {
        url: string
      }
    }
  }
};
