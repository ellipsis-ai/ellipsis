// @flow

define(function(require) {
  const React = require('react'),
    diffs = require('../../models/diffs');

  type Props = {
    part: diffs.TextPart
  };

  class TextDiffPart extends React.PureComponent<Props> {
    props: Props;

    getPartClass(): string {
      const kind = this.props.part.kind;
      if (kind === "removed") {
        return "bg-pink-light type-pink-dark border border-faint";
      } else if (kind === "added") {
        return "bg-green-light border border-faint";
      } else {
        return "";
      }
    }

    getText(): string {
      return this.props.part.value;
    }

    render(): React.Node {
      return (
        <span className={`type-preserve-spaces ${this.getPartClass()}`}>
          {this.getText()}
          {this.props.part.endsWithNewLine ? (
            <span className="type-weak">↩︎</span>
          ) : null}
        </span>
      );
    }
  }

  return TextDiffPart;
});
