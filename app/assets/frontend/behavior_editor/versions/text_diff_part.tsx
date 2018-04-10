import * as React from 'react';
import {TextPart} from '../../models/diffs';

type Props = {
  part: TextPart
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

  render() {
    const pieces = this.props.part.value.split("\n");
    return (
      <span className={`type-preserve-spaces ${this.getPartClass()}`}>
          {pieces.map((text, index) => (
            <span key={`part${index}`}>
              {text}
              {index + 1 < pieces.length ? (
                <span className="type-weak">↩︎</span>
              ) : null}
            </span>
          ))}
        </span>
    );
  }
}

export default TextDiffPart;

