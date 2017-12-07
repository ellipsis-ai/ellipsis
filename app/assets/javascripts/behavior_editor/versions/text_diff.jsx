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
    parts: Array<TextPart>,
    isCode: boolean
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

    renderBlankLine(onlyLine: boolean): React.Node {
      return onlyLine ? (
        <div className="type-disabled type-italic">(blank)</div>
      ) : (
        <div>&nbsp;</div>
      );
    }

    renderLine(line: Array<TextPart>, onlyLine: boolean): React.Node {
      const isEmpty = line.length === 0 || line.length === 1 && line[0].valueIsEmpty();
      return (
        <div className={`type-wrap-words ${this.props.isCode ? "type-monospace" : ""}`}>
          {isEmpty ?
            this.renderBlankLine(onlyLine) :
            line.map((part, partIndex) => (
              <TextDiffPart key={`part${partIndex}`} part={part} />
            ))}
        </div>
      );
    }

    renderLines(oldParts, newParts, totalLines: number): React.Node {
      const maxNumDigits = String(totalLines).length;
      const lineCounter = new Array(totalLines).fill("");
      const oldIsOneLine = oldParts.length === 1;
      const newIsOneLine = newParts.length === 1;
      return lineCounter.map((nothing, lineIndex) => {
        const lineNumber = Formatter.leftPad(lineIndex + 1, maxNumDigits);
        const isLastLine = lineIndex + 1 === totalLines;
        const lineClass = isLastLine ? "border-bottom" : "";
        const oldPart = oldParts[lineIndex];
        const newPart = newParts[lineIndex];
        return (
          <div key={`line${lineIndex}`} className="column-row">
            {totalLines > 1 ? (
              <div className="column column-shrink type-monospace type-weak bg-light phxs border-bottom">
                {oldPart ? lineNumber : ""}
              </div>
            ) : null}
            <div className={`column column-one-half phxs ${lineClass}`}>
              {oldPart ? this.renderLine(oldPart, oldIsOneLine && lineIndex === 0) : null}
            </div>
            {totalLines > 1 ? (
              <div className="column column-shrink type-monospace type-weak bg-light phxs border-bottom border-left">
                {newPart ? lineNumber : ""}
              </div>
            ) : null}
            <div className={`column column-one-half phxs ${lineClass} ${totalLines === 1 ? "border-left" : ""}`}>
              {newPart ? this.renderLine(newPart, newIsOneLine && lineIndex === 0) : null}
            </div>
          </div>
        );
      });
    }

    render(): React.Node {
      const oldPartsByLine = this.getPartsByLine(this.getOldParts());
      const newPartsByLine = this.getPartsByLine(this.getNewParts());
      const totalLines = Math.max(oldPartsByLine.length, newPartsByLine.length);
      return (
        <div className={this.props.className || ""}>
          <div className="columns columns-elastic">
            <div className="column-group">
              {this.renderLines(oldPartsByLine, newPartsByLine, totalLines)}
            </div>
          </div>
        </div>
      );
    }
  }

  return TextDiff;
});
