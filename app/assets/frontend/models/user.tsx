export interface UserJson {
  id: string,
  userName?: Option<string>;
  fullName?: Option<string>;
  tz?: Option<string>;
  teamName?: Option<string>;
  email?: Option<string>;
}

interface UserInterface extends UserJson {}

class User {
    constructor(
      readonly id: string,
      readonly userName: Option<string>,
      readonly fullName: Option<string>,
      readonly tz: Option<string>,
      readonly teamName: Option<string>,
      readonly email: Option<string>
    ) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        userName: { value: userName, enumerable: true },
        fullName: { value: fullName, enumerable: true },
        tz: { value: tz, enumerable: true },
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
      return Boolean(otherUser && this.id === otherUser.id);
    }

    static fromProps(props: UserInterface): User {
      return new User(
        props.id,
        props.userName,
        props.fullName,
        props.tz,
        props.teamName,
        props.email
      );
    }

    static fromJson(props: UserJson): User {
      return User.fromProps(props);
    }
}

export default User;
