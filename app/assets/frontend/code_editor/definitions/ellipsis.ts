import {RequiredOAuthApplication} from "../../models/oauth";
import {RequiredAWSConfig} from "../../models/aws";

const ACTION_INFO_TYPE = 'ActionInfo';
const DATA_TYPE_INFO_TYPE = 'DataTypeInfo';

interface Props {
  requiredAWSConfigs: Array<RequiredAWSConfig>,
  oauthApiApplications: Array<RequiredOAuthApplication>,
  envVariableNames: Array<string>,
  paramTypeIds: Array<string>,
  isDataType: boolean
}

const EllipsisObjectDefinitions = {
  buildFor(props: Props): string {
    return `
/**
 * The ellipsis object is passed to every action or data type at run time, 
 * and contains useful contextual data and methods used to control execution
 */
declare namespace ellipsis {

  export interface ActionArg {
  
    /** The name of a parameter to set */
    name: string
    
    /** The value of a parameter to set */
    value: string
    
  }

  export interface NextAction {
  
    /** The name of the action to run next */
    actionName: string
    
    /** Any arguments to be passed to the action */
    args?: ActionArg[]
  }

  export interface ActionChoice {

    /** The label the user will see to trigger this action */ 
    label: string
    
    /** The name of the action to run */
    actionName: string
    
    /** Any arguments to set for this action’s function */
    args?: ActionArg[]
    
    /** Whether other people in the channel can trigger this action */ 
    allowOthers?: boolean
    
    /** Whether this choice can be triggered multiple times */
    allowMultipleSelections?: boolean
    
    /** Whether to provide feedback about who triggered this action in a channel */
    quiet?: boolean
    
    /** ID of another skill on the same team */
    skillId?: string
    
  }

  export interface InlineFileAttachment {
  
    /** The content of the file, as a string */
    content: string
    
    /** An optional filetype, e.g. "text" or "jpg" */
    filetype?: string
  }

  export interface NamedFileAttachment {
  
    /** The name of the file being attached */
    filename: string
    
    /** An optional filetype, e.g. "text" or "jpg" */
    filetype?: string
  }

  export type FileAttachment = InlineFileAttachment | NamedFileAttachment

  export interface AWSConfig {
  
    /** The access key ID for this AWS configuration */
    accessKeyId: string
    
    /** The secret access key for this AWS configuration */ 
    secretAccessKey: string
    
    /** The region for this AWS configuration, e.g. "us-east-1" */
    region: string
  }

  export interface OAuthLink {
    externalSystem: string
    token: string
    oauthToken: string
  }
  
  export interface EventUser {
    links: OAuthLink[]
  }

  export interface DeprecatedUserInfo {
    links: OAuthLink[]
    messageInfo: DeprecatedMessageInfo
  }

  export interface UserData {
    /** How the user is using Ellipsis, e.g. "slack" */
    context: string
    
    /** The user’s user name according to the context */
    userName: string
    
    /** The user’s user ID for Ellipsis (regardless of context) */
    ellipsisUserId?: string
    
    /** The user’s user ID for this context */
    userIdForContext?: string
    
    /** The user’s full name, if known */
    fullName?: string
    
    /** The user’s email address, if known */
    email?: string
    
    /** The user’s time zone name in tz database format, e.g. "America/Los_Angeles" */
    timeZone?: string
    
    /** A link for the user, formatted for the specific context */
    formattedLink? string
  }
  
  /**
   * Run-time type IDs for params, including buitins and ID
   */ 
  type ParamType = ${props.paramTypeIds.map(ea => `'${ea}'`).join(" | ")};
  
  export interface InputInfo {
  
    /** The name of the inputs, passed to the action code as a parameter */
    name: string
    
    /** The type of the input. Can be builtins like Text, Number, etc or the ID for a custom data type on this skill */
    paramType: ParamType
    
    /** The question asked of the user in chat */
    question: string
  }
  
  export interface ${ACTION_INFO_TYPE} {

    /** The name of the action, if any */
    name?: string

    /** The description of the action, if any */
    description?: string
    
    /** The inputs that need to be filled in to run the action */
    inputs: InputInfo[]

  }
  
  export interface DefaultStorageFieldInfo {
  
    /** The name of the field */
    name: string
    
    /** The type of the field */
    fieldType: ParamType
    
    /** Whether or not the field acts as the label for the type */
    isLabel: boolean
    
  }
  
  export interface ${DATA_TYPE_INFO_TYPE} {
  
    /** The id of the data type */
    id: string
    
    /** The name of the data type, if any */
    name?: string
    
    /** The inputs that need to be filled in to use the data type */
    inputs: InputInfo[]
    
    /** Whether or not the data type uses code to produce options */
    usesCode: boolean
    
    /** If backed by default storage, rather than code, these are the default storage fields */
    defaultStorageFields: DefaultStorageFieldInfo[]
  }
  
  export interface SkillInfo {

    /** The name of the skill, if any */
    name?: string

    /** The description of the skill, if any */
    description?: string

    /** The icon for the skill, if any */
    icon?: string

    /** The skill's actions */
    actions: ${ACTION_INFO_TYPE}[]
    
    /** The skill's data types */
    dataTypes: ${DATA_TYPE_INFO_TYPE}[]

    /** The creation timestamp for this version of the skill */
    createdAt: Date

    /** The author of the skill, if known */
    author?: UserData
  }
  
  export interface MetaInfo {

    /** The current action or data type being invoked */
    current: ${props.isDataType ? DATA_TYPE_INFO_TYPE : ACTION_INFO_TYPE } 
  
    /** The skill containing the action or data type being invoked */
    skill: SkillInfo
    
  }

  export interface DeprecatedMessageInfo {
    /** The text of the message that preceded this action */
    text: string
    
    /** The name of the message medium, e.g. "slack" or "msTeams" */
    medium: string

    /** The human-readable description of the message medium, e.g. "Slack" or "Microsoft Teams" */
    mediumDescription: string
    
    /** The channel ID for where this message originated, if applicable */
    channel?: string
    
    /** The thread ID for where this message originated, if applicable */
    thread?: string
    
    /** The user’s ID for this medium */
    userId: string
    
    /** List of info about users mentioned in this message, if any */
    usersMentioned: UserData[]
    
    /** Link to this message, if applicable */
    permalink?: string
    
    /** Additional contextual details about this message */
    details: ContextMessageDetails
    reactionAdded?: string
  }
  
  export interface Channel {
    id: string
    name?: string
    formattedLink?: string
    members?: UserData[]
  }
  
  export interface Message {
    /** The text of the message that preceded this action */
    text: string
    
    /** The channel ID for where this message originated, if applicable */
    channel?: Channel
    
    /** The thread ID for where this message originated, if applicable */
    thread?: string
    
    /** List of info about users mentioned in this message, if any */
    usersMentioned: UserData[]
    
    /** Link to this message, if applicable */
    permalink?: string
    
    /** The reaction added to the message, if any */
    reactionAdded?: string
  }

  export interface ContextMessageDetails {
    /** List of user IDs in this channel, if applicable */
    channelMembers?: string[]
    
    /** Name of this channel, if applicable */
    channelName?: string
    
    /** Name of the author of the message, if applicable */
    name?: string
    
    /** Contextual details about the author of the message, if applicable */
    profile?: ContextUserProfile
  }

  export interface ContextUserProfile {
    /** The author’s name as displayed to others */
    displayName?: string
    
    /** The author’s first name */
    firstName?: string
    
    /** The author’s last name */
    lastName?: string
    
    /** The author’s whole name */
    realName?: string
    
    /** The author’s email address */
    email?: string
    
    /** The author’s phone number */
    phone?: string
  }

  interface Schedule {
    /** A link to edit the schedule that triggered this action */
    editLink: string

    /** Description of how this schedule is configured to repeat */
    recurrence: string
  }

  interface EllipsisError extends Error {
    /** An error message appropriate to display to users of this skill */
    userMessage: string | null
  }

  interface EllipsisErrorConstructor {
    new (error: Error | string, options?: {
      userMessage?: string
    }): EllipsisError;
  }
  
  /**
   * Require an NPM module with a particular version, e.g.:
   * @param moduleName - An NPM module, optionally with a version, e.g. \`request@2.88.0\`
   * @returns The module requested
   */
  function require(moduleName: string): any

  /**
   * End the action successfully and deliver \`successResult\`.
   * @param successResult - The output, either processed by the template for actions, or presented as choices for data types
   * @param options - Attach files to the result or set actions to run next, if any
   */    
  function success(successResult: any, options?: {
  
    /** Set an action to run immediately at the conclusion of this one */ 
    next?: NextAction
    
    /** Present the user with a list of actions to run at the conclusion of this one */  
    choices?: ActionChoice[]
    
    /** Attach 1 or more files to the result of this action */
    files?: FileAttachment[]
  }): void

  /**
   * End the action successfully without delivering any response.
   */
  function noResponse(): void

  /**
   * End the action with an error message.
   * @param error - A message or instance of \`Error\` that describes the problem encountered. This will be logged and displayed in developer mode.
   * @param options - Set \`userMessage\` to a string to describe the problem in user-friendly terms.
   */  
  function error(error: Error | string, options?: {
    userMessage?: string
  }): EllipsisError

  /**
   * Class to instantiate errors that can be thrown with user-friendly messages\n
   * \`throw new ellipsis.Error("Couldn't reach server", { userMessage: "The server is not responding." })\`
   */
  const Error: EllipsisErrorConstructor

  /**
   * Run-time token that can be used to call the Ellipsis API
   */
  const token: string

  /**
   * Run-time OAuth user-specific tokens for connections to third-party services
   */ 
  const accessTokens: {
    ${props.oauthApiApplications.map((ea) => `${ea.nameInCode}: string`).join(",\n")}
  }

  /**
   * Contains all of the environment variables available at run-time
   */
  const env: {
    ${props.envVariableNames.map((ea) => `${ea}: string`).join(",\n")}
  }

  /**
   * Contains information needed to use any available AWS configurations
   */
  const aws: {
    ${props.requiredAWSConfigs.map(ea => `${ea.nameInCode}: AWSConfig`).join(",\n")}
  }

  /**
   * Information about the team’s Ellipsis instance (deprecated key: use \`ellipsis.team\` instead)
   */
  const teamInfo: {
    links: OAuthLink[]
    
    /** The bot’s name in the platform used to run the action */ 
    botName: string
    
    /** The bot’s user ID in the platform used to run the action */
    botUserIdForContext: string
    
    /** The team’s current time zone in tz database format, e.g. \`America/New_York\` */
    timeZone: string
  }
  
    /**
   * Information about the team’s Ellipsis instance
   */
  const team: {
    links: OAuthLink[]
    
    /** The bot’s name in the platform used to run the action */ 
    botName: string
    
    /** The bot’s user ID in the platform used to run the action */
    botUserIdForContext: string
    
    /** The team’s current time zone in tz database format, e.g. \`America/New_York\` */
    timeZone: string
  }

  /**
   * Information about the user who ran the action (deprecated: use \`ellipsis.event.user\` instead)
   */
  const userInfo: DeprecatedUserInfo & UserData

  type EventType = "scheduled" | "api" | "test" | "chat" | "web"

  const event: {
    /**
     * How the initial action at the start of any sequence was triggered. 
     * (This may not be how this action was triggered, if another action triggered this one.)
     */
    originalEventType: EventType
    
    /** The user who triggered the event */
    user: EventUser & UserData
    
    /** The name of the event platform, e.g. "slack" or "msTeams" */
    platformName: string

    /** The human-readable description of the event platform, e.g. "Slack" or "Microsoft Teams" */
    platformDescription: string
    
    /** The associated message, if a message event */
    message?: Message

    /** Information about the schedule that triggered this event, if applicable */ 
    schedule?: Schedule
  }
  
  const meta: MetaInfo {
    current: ${props.isDataType ? DATA_TYPE_INFO_TYPE : ACTION_INFO_TYPE},
    skill: SkillInfo
  }
}
`;
  }
};

export default EllipsisObjectDefinitions;
