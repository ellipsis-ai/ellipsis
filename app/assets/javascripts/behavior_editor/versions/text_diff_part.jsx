// @flow
import type {Element} from 'react';

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

    getText(): Array<Element<'span'>> {
      const text = this.props.part.value;
      const lines = text.split("\n");
      return lines.map((ea, index) => (
        <span key={`line${index}`}>{ea}{index + 1 < lines.length ? (
          <br />
        ) : null}</span>
      ));
    }

    render(): React.Node {
      return (
        <span className={this.getPartClass()}>{this.getText()}</span>
      );
    }
  }

  return TextDiffPart;
});
