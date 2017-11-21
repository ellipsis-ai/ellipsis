// @flow
define(function(require) {
  const React = require('react'),
    LinkedGithubRepo = require('../../models/linked_github_repo');

  type Props = {
    linked?: LinkedGithubRepo
  }

  class GithubOwnerRepoReadonly extends React.PureComponent<Props> {
    props: Props;

    render(): React.Node {
      if (this.props.linked) {
        const linked = this.props.linked;
        const path = linked.getOwnerAndRepo();
        const url = linked.getUrl();
        return (
          <a href={url} target="github" className="type-s type-monospace">{path}</a>
        );
      } else {
        return null;
      }
    }
  }

  return GithubOwnerRepoReadonly;
});
