// @flow
define(function(require) {
  const React = require('react'),
    Formatter = require('../../lib/formatter'),
    TextDiffPart = require('./text_diff_part'),
    diffs = require('../../models/diffs');
  const TextPart = diffs.TextPart;
  const DiffConstants = diffs.constants;

  type Props = {
    className: ?string,
    parts: Array<TextPart>
  };

  class TextDiff extends React.PureComponent<Props> {
    props: Props;

    newPartFromOldPartValue(value: string, kind: string): TextPart {
      return new TextPart(value, kind === DiffConstants.TEXT_ADDED, kind === DiffConstants.TEXT_REMOVED);
    }

    getPartsByLine(parts: Array<TextPart>): Array<Array<TextPart>> {
      const lines = [[]];
      parts.forEach((part) => {
        const partLines = part.value.split("\n");
        const lastLineIndex = Math.max(lines.length - 1);
        lines[lastLineIndex].push(this.newPartFromOldPartValue(partLines[0], part.kind));
        partLines.slice(1).forEach((partLine) => {
          lines.push([this.newPartFromOldPartValue(partLine, part.kind)]);
        });
      });
      return lines;
    }

    getOldParts(): Array<TextPart> {
      return this.props.parts.filter((ea) => ea.kind === DiffConstants.TEXT_REMOVED || ea.kind === DiffConstants.TEXT_UNCHANGED);
    }

    getNewParts(): Array<TextPart> {
      return this.props.parts.filter((ea) => ea.kind === DiffConstants.TEXT_ADDED || ea.kind === DiffConstants.TEXT_UNCHANGED);
    }

    renderLines(line: Array<TextPart>, lineIndex: number, totalLines: number): React.Node {
      return (
        <div className="columns columns-elastic" key={`line${lineIndex}`}>
          {totalLines > 1 ? (
            <div className="column column-shrink type-monospace type-weak bg-light paxs">
              {Formatter.leftPad(lineIndex + 1, String(totalLines).length)}
            </div>
          ) : null}
          <div className="column column-expand paxs">
            {line.map((part, partIndex) => (
              <TextDiffPart key={`part${partIndex}`} part={part} />
            ))}
          </div>
        </div>
      );
    }

    render(): React.Node {
      const oldPartsByLine = this.getPartsByLine(this.getOldParts());
      const newPartsByLine = this.getPartsByLine(this.getNewParts());
      const totalLines = Math.max(oldPartsByLine.length, newPartsByLine.length);
      return (
        <div className={this.props.className || ""}>
          <div className="columns">
            <div className="column column-one-half">
              {oldPartsByLine.map((line, index) => this.renderLines(line, index, totalLines))}
            </div>
            <div className="column column-one-half border-left">
              {newPartsByLine.map((line, index) => this.renderLines(line, index, totalLines))}
            </div>
          </div>
        </div>
      );
    }
  }

  return TextDiff;
});
