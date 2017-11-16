define(function() {
  class LinkedGithubRepo {
    constructor(props) {
      Object.defineProperties(this, {
        owner: { value: props.owner, enumerable: true },
        repo: { value: props.repo, enumerable: true }
      });
    }

    getOwner() {
      return this.owner || "";
    }

    getRepo() {
      return this.repo || "";
    }

    getPath() {
      return `${this.getOwner()}/${this.getRepo()}`;
    }

    clone(props) {
      return new LinkedGithubRepo(Object.assign({}, this, props));
    }

    static fromJson(props) {
      return new LinkedGithubRepo(props);
    }

  }

  return LinkedGithubRepo;
});
