import {RequiredOAuthApplication} from "../../models/oauth";
import {RequiredAWSConfig} from "../../models/aws";

interface Props {
  requiredAWSConfigs: Array<RequiredAWSConfig>,
  oauthApiApplications: Array<RequiredOAuthApplication>,
  envVariableNames: Array<string>
}

const EllipsisObjectDefinitions = {
  buildFor(props: Props): string {
    return `
declare namespace ellipsis {

  export interface ActionArg {
    name: string
    value: any
  }

  export interface NextAction {
    actionName: string
    args?: ActionArg[]
  }

  export interface ActionChoice {
    label: string
    actionName: string
    args?: ActionArg[]
    allowOthers?: boolean
    allowMultipleSelections?: boolean
    quiet?: boolean
  }

  export interface InlineFileAttachment {
    content: string
    filetype?: string
  }

  export interface NamedFileAttachment {
    filename: string
    filetype?: string
  }

  export type FileAttachment = InlineFileAttachment | NamedFileAttachment

  export interface SuccessOptions {
    next?: NextAction
    choices?: ActionChoice[]
    files?: FileAttachment[]
  }

  export interface AWSConfig {
    accessKeyId: string
    secretAccessKey: string
    region: string
  }

  export interface OAuthLink {
    externalSystem: string
    token: string
    oauthToken: string
  }

  export interface UserInfo {
    links: OAuthLink[]
    messageInfo: MessageInfo
  }

  export interface UserData {
    context: string
    userName: string
    ellipsisUserId?: string
    userIdForContext?: string
    fullName?: string
    email?: string
    timeZone?: string
  }

  export interface MessageInfo {
    text: string
    medium: string
    channel?: string
    thread?: string
    userId: string
    usersMentioned: UserData[]
    permalink?: string
    details: ContextMessageDetails
  }

  export interface ContextMessageDetails {
    channelMembers?: string[]
    channelName?: string
    name?: string
    profile?: ContextUserProfile
  }

  export interface ContextUserProfile {
    displayName?: string
    firstName?: string
    lastName?: string
    realName?: string
    email?: string
    phone?: string
  }

  interface EllipsisError extends Error {
    userMessage: string
  }

  interface EllipsisErrorConstructor {
    new (error: Error | string, options?: {
      userMessage?: string
    }): EllipsisError;
  }

  function require(moduleName: string): any

  function success(successResult: any, options?: SuccessOptions): void

  function noResponse(): void

  function error(error: Error | string, options?: {
    userMessage?: string
  }): EllipsisError

  const Error: EllipsisErrorConstructor

  const token: string

  const accessTokens: {
    ${props.oauthApiApplications.map((ea) => `${ea.nameInCode}: string`).join(",\n")}
  }

  const env: {
    ${props.envVariableNames.map((ea) => `${ea}: string`).join(",\n")}
  }

  const aws: {
    ${props.requiredAWSConfigs.map(ea => `${ea.nameInCode}: AWSConfig`).join(",\n")}
  }

  const teamInfo: {
    links: OAuthLink[]
    botName: string
    botUserIdForContext: string
    timeZone: string
  }

  const userInfo: UserInfo & UserData

  type EventType = "scheduled" | "api" | "test" | "chat" | "web"

  const event: {
    originalEventType: EventType
  }
}
`;
  }
}

export default EllipsisObjectDefinitions;
