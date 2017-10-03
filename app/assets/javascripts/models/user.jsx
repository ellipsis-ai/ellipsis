define(function() {

  class User {
    constructor(props) {
      const defaultProps = Object.assign({}, props);
      Object.defineProperties(this, {
        id: { value: defaultProps.id, enumerable: true },
        userName: { value: defaultProps.userName, enumerable: true },
        fullName: { value: defaultProps.fullName, enumerable: true }
      });
    }

    formattedUserName() {
      return this.userName ? `@${this.userName}` : "Unknown";
    }

    formattedName() {
      return this.formattedNameIfKnown() || "Unknown";
    }

    formattedNameIfKnown() {
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

    formattedFullNameOrUserName() {
      return this.fullName || this.formattedUserName();
    }

    static fromJson(props) {
      return new User(props);
    }
  }

  return User;

});
