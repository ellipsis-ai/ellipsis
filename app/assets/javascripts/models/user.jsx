define(function() {

  class User {
    constructor(props) {
      Object.defineProperties(this, {
        id: { value: props.id, enumerable: true },
        accountName: { value: props.accountName, enumerable: true },
        accountType: { value: props.accountType, enumerable: true },
        accountId: { value: props.accountId, enumerable: true }
      });
    }

    formattedName() {
      return this.accountName ? `@${this.accountName}` : "Unknown";
    }

    static fromJson(props) {
      return new User(props);
    }
  }

  return User;

});
