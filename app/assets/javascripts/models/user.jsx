define(function() {

  class User {
    constructor(props) {
      const defaultProps = Object.assign({}, props);
      Object.defineProperties(this, {
        id: { value: defaultProps.id, enumerable: true },
        accountName: { value: defaultProps.accountName, enumerable: true },
        accountType: { value: defaultProps.accountType, enumerable: true },
        accountId: { value: defaultProps.accountId, enumerable: true }
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
