export enum GithubFetchErrorType {
  NoCommiterInfoFound = "NoCommiterInfoFound",
  NoRepoFound = "NoRepoFound",
  NoBranchFound = "NoBranchFound",
  NoCommitFound = "NoCommitFound",
  NoValidSkillFound = "NoValidSkillFound"
}

export interface GithubFetchError {
  message: string
  type?: GithubFetchErrorType,
  details?: { [k: string]: any }
}
