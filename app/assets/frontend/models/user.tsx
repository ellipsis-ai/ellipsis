export interface UserJson {
  id: string,
  userName: string | null;
  fullName: string | null;
}

interface UserInterface extends UserJson {}

class User {
    constructor(
      readonly id: string,
      readonly userName: string | null,
      readonly fullName: string | null
    ) {
      Object.defineProperties(this, {
        id: { value: id, enumerable: true },
        userName: { value: userName, enumerable: true },
        fullName: { value: fullName, enumerable: true }
      });
    }

    formattedUserName(): string {
      return this.userName ? `@${this.userName}` : "Unknown";
    }

    formattedName(): string {
      return this.formattedNameIfKnown() || "Unknown";
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

    formattedFullNameOrUserName(): string {
      return this.fullName || this.formattedUserName();
    }

    isSameUser(otherUser?: User | null): boolean {
      return Boolean(otherUser && this.id === otherUser.id);
    }

    static fromProps(props: UserInterface): User {
      return new User(
        props.id,
        props.userName,
        props.fullName
      );
    }

    static fromJson(props: UserJson): User {
      return User.fromProps(props);
    }
}

export default User;
