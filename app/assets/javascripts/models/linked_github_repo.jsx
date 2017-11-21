// @flow
define(function() {
  class LinkedGithubRepo {
    owner: string;
    repo: string;

    constructor(props) {
      Object.defineProperties(this, {
        owner: { value: props.owner, enumerable: true },
        repo: { value: props.repo, enumerable: true }
      });
    }

    getOwner(): string {
      return this.owner || "";
    }

    getRepo(): string {
      return this.repo || "";
    }

    getOwnerAndRepo(): string {
      return `${this.getOwner()}/${this.getRepo()}`;
    }

    getPath(): string {
      return `github.com/${this.getOwnerAndRepo()}`;
    }

    getUrl(): string {
      return `https://${this.getPath()}`;
    }

    clone(props): LinkedGithubRepo {
      return new LinkedGithubRepo(Object.assign({}, this, props));
    }

    static fromJson(props): LinkedGithubRepo {
      return new LinkedGithubRepo(props);
    }

  }

  return LinkedGithubRepo;
});
