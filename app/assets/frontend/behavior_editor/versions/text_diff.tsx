import * as React from 'react';
import Formatter from '../../lib/formatter';
import TextDiffPart from './text_diff_part';
import {MultiLineTextPropertyDiff, TextPart} from '../../models/diffs';

type Props = {
  className: string | null,
  diff: MultiLineTextPropertyDiff
};

class TextDiff extends React.PureComponent<Props> {
    props: Props;

    renderBlankLine(onlyLine: boolean) {
      return onlyLine ? (
        <div className="type-disabled type-italic">(blank)</div>
      ) : (
        <div>&nbsp;</div>
      );
    }

    getBgColorForLine(line?: Array<TextPart> | null): string {
      if (line && line.some((part) => part.isRemoved())) {
        return "bg-pink-lightest";
      } else if (line && line.some((part) => part.isAdded())) {
        return "bg-green-lightest";
      } else {
        return "";
      }
    }

    renderLine(line: Array<TextPart> | null | undefined, onlyLine: boolean) {
      const isEmpty = !line || line.length === 0 || line.length === 1 && line[0].valueIsEmpty();
      return (
        <div className={`type-wrap-words ${this.props.diff.isCode ? "type-monospace" : ""} ${this.getBgColorForLine(line)}`}>
          {isEmpty ?
            this.renderBlankLine(onlyLine) :
            line && line.map((part, partIndex) => (
              <TextDiffPart key={`part${partIndex}`} part={part} />
            ))}
        </div>
      );
    }

    renderLines(diff: MultiLineTextPropertyDiff) {
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

    render() {
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

export default TextDiff;

