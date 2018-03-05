class LinkedGithubRepo {
    owner: string;
    repo: string;
    currentBranch: string;

    constructor(owner: string, repo: string, currentBranch: string) {
      Object.defineProperties(this, {
        owner: { value: owner, enumerable: true },
        repo: { value: repo, enumerable: true },
        currentBranch: { value: currentBranch, enumerable: true }
      });
    }

    getOwner(): string {
      return this.owner;
    }

    getRepo(): string {
      return this.repo;
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

    clone(props: {}): LinkedGithubRepo {
      return LinkedGithubRepo.fromProps(Object.assign({}, this, props));
    }

    static fromProps(props): LinkedGithubRepo {
      return new LinkedGithubRepo(
        props.owner || "",
        props.repo || "",
        props.currentBranch || ""
      );
    }

    static fromJson(props): LinkedGithubRepo {
      return LinkedGithubRepo.fromProps(Object.assign({}, props));
    }

}

export default LinkedGithubRepo;

