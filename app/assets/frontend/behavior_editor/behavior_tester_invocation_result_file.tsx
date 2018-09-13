import * as React from 'react';

interface Props {
  filename?: Option<string>,
  filetype?: Option<string>,
  content?: Option<string>
}

class BehaviorTesterInvocationResultFile extends React.PureComponent<Props> {
    contentWasTruncated(): boolean {
      return Boolean(this.props.content && this.props.content.length > 1000);
    }

    contentIsEmpty(): boolean {
      return !this.props.content || this.props.content.length === 0;
    }

    render() {
      return (
        <div className="border">
          <div className="bg-light type-weak phxs">
            <b>{this.props.filename || ""}</b>
            <span>{this.props.filetype ? ` (${this.props.filetype} file)` : "(file)"}:</span>
          </div>
          <div className="phxs pbxs">
            <pre>{this.props.content ? this.props.content.slice(0, 1000) : ""}</pre>
            <div>{this.contentWasTruncated() ? (
              <span className="type-disabled">(truncated to 1000 characters)</span>
            ) : ""}</div>
            <div>{this.contentIsEmpty() ? (
              <span className="type-disabled">(empty)</span>
            ) : null}</div>
          </div>
        </div>
      );
    }
}

export default BehaviorTesterInvocationResultFile;

