define(function() {

  class User {
    constructor(props) {
      const defaultProps = Object.assign({}, props);
      Object.defineProperties(this, {
        id: { value: defaultProps.id, enumerable: true },
        name: { value: defaultProps.name, enumerable: true }
      });
    }

    formattedName() {
      return this.name ? `@${this.name}` : "Unknown";
    }

    static fromJson(props) {
      return new User(props);
    }
  }

  return User;

});
