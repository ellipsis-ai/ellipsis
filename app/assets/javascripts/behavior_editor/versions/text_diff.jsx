// @flow
define(function(require) {
  const React = require('react'),
    Formatter = require('../../lib/formatter'),
    TextDiffPart = require('./text_diff_part'),
    diffs = require('../../models/diffs');
  const TextPart = diffs.TextPart;

  type Props = {
    className: ?string,
    diff: diffs.MultiLineTextPropertyDiff
  };

  class TextDiff extends React.PureComponent<Props> {
    props: Props;

    renderBlankLine(onlyLine: boolean): React.Node {
      return onlyLine ? (
        <div className="type-disabled type-italic">(blank)</div>
      ) : (
        <div>&nbsp;</div>
      );
    }

    renderLine(line?: Array<TextPart>, onlyLine: boolean): React.Node {
      const isEmpty = !line || line.length === 0 || line.length === 1 && line[0].valueIsEmpty();
      return (
        <div className={`type-wrap-words ${this.props.diff.isCode ? "type-monospace" : ""}`}>
          {isEmpty ?
            this.renderBlankLine(onlyLine) :
            line && line.map((part, partIndex) => (
              <TextDiffPart key={`part${partIndex}`} part={part} />
            ))}
        </div>
      );
    }

    renderLines(diff): React.Node {
      const totalLines = Math.max(diff.oldLines.length, diff.newLines.length);
      const longerSide = diff.oldLines.length > diff.newLines.length ? diff.oldLines : diff.newLines;
      const maxNumDigits = String(totalLines).length;
      const oldIsOneLine = diff.oldLines.filter((line) => line.length > 0).length < 2;
      const newIsOneLine = diff.newLines.filter((line) => line.length > 0).length < 2;
      let oldLineIndex = 0;
      let newLineIndex = 0;
      return longerSide.map((nothing, lineIndex) => {
        const isLastLine = lineIndex + 1 === totalLines;
        const lineClass = isLastLine ? "border-bottom" : "";
        const oldLine = diff.oldLines[lineIndex];
        const oldLineExists = oldLine && oldLine.length > 0;
        const oldLineNumber = oldLineExists ? Formatter.leftPad(++oldLineIndex, maxNumDigits) : "";
        const newLine = diff.newLines[lineIndex];
        const newLineExists = newLine && newLine.length > 0;
        const newLineNumber = newLineExists ? Formatter.leftPad(++newLineIndex, maxNumDigits) : "";
        return (
          <div key={`line${lineIndex}`} className="column-row">
            {totalLines > 1 ? (
              <div className="column column-shrink type-monospace type-weak bg-light phxs border-bottom">
                {oldLineNumber}
              </div>
            ) : null}
            <div className={`column column-one-half phxs ${lineClass}`}>
              {this.renderLine(oldLine, oldIsOneLine && lineIndex === 0)}
            </div>
            {totalLines > 1 ? (
              <div className="column column-shrink type-monospace type-weak bg-light phxs border-bottom border-left">
                {newLineNumber}
              </div>
            ) : null}
            <div className={`column column-one-half phxs ${lineClass} ${totalLines === 1 ? "border-left" : ""}`}>
              {this.renderLine(newLine, newIsOneLine && lineIndex === 0)}
            </div>
          </div>
        );
      });
    }

    render(): React.Node {
      return (
        <div className={this.props.className || ""}>
          <div className="columns columns-elastic">
            <div className="column-group">
              {this.renderLines(this.props.diff)}
            </div>
          </div>
        </div>
      );
    }
  }

  return TextDiff;
});
