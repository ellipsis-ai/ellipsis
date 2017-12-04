// @flow
define(function(require) {
  const React = require('react'),
    TextDiffPart = require('./text_diff_part'),
    diffs = require('../../models/diffs');

  type Props = {
    className: ?string,
    parts: Array<diffs.TextPart>
  };

  class TextDiff extends React.PureComponent<Props> {
    props: Props;

    render(): React.Node {
      return (
        <div className={this.props.className || ""}>
          {this.props.parts.map((ea, index) => (
            <TextDiffPart key={`part${index}`} part={ea} />
          ))}
        </div>
      );
    }
  }

  return TextDiff;
});
