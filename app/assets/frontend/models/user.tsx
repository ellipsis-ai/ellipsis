export interface UserJson {
  ellipsisUserId: string;
  userName?: Option<string>;
  fullName?: Option<string>;
  timeZone?: Option<string>;
  teamName?: Option<string>;
  email?: Option<string>;
}

interface UserInterface extends UserJson {}

class User {
    constructor(
      readonly ellipsisUserId: string,
      readonly userName: Option<string>,
      readonly fullName: Option<string>,
      readonly timeZone: Option<string>,
      readonly teamName: Option<string>,
      readonly email: Option<string>
    ) {
      Object.defineProperties(this, {
        ellipsisUserId: { value: ellipsisUserId, enumerable: true },
        userName: { value: userName, enumerable: true },
        fullName: { value: fullName, enumerable: true },
        timeZone: { value: timeZone, enumerable: true },
        teamName: { value: teamName, enumerable: true },
        email: { value: email, enumerable: true }
      });
    }

    formattedUserName(defaultName?: string): string {
      return this.userName ? `@${this.userName}` : (defaultName || "Unknown");
    }

    formattedName(defaultName?: string): string {
      return this.formattedNameIfKnown() || (defaultName || "Unknown");
    }

    formattedNameIfKnown(): string {
      if (this.fullName && this.userName) {
        return `${this.fullName} (@${this.userName})`;
      } else if (this.fullName) {
        return this.fullName;
      } else if (this.userName) {
        return this.formattedUserName();
      } else {
        return "";
      }
    }

    formattedFullNameOrUserName(defaultName?: string): string {
      return this.fullName || this.formattedUserName(defaultName);
    }

    isSameUser(otherUser?: Option<User>): boolean {
      return Boolean(otherUser && this.ellipsisUserId === otherUser.ellipsisUserId);
    }

    static fromProps(props: UserInterface): User {
      return new User(
        props.ellipsisUserId,
        props.userName,
        props.fullName,
        props.timeZone,
        props.teamName,
        props.email
      );
    }

    static fromJson(props: UserJson): User {
      return User.fromProps(props);
    }

    static withoutProfile(userId: string): User {
      return User.fromProps({ ellipsisUserId: userId });
    }
}

export default User;
