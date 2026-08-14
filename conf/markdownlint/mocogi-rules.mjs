// Note(MD4C7A): Keep these headings in sync with Note(MD4C7A) in
// ContentMarkdownPrinter, ModuleContentParser, and the markdown message labels.
const generatedHeading =
  /^## \((?:de|en)\) (?:Angestrebte Lernergebnisse|Learning Outcome|Modulinhalte|Module Content|Lehr- und Lernmethoden \(Medienformen\)|Teaching and Learning Methods|Empfohlene Literatur|Recommended Reading|Besonderheiten|Particularities):$/;
const atxHeading = /^( {0,3})(#{1,6})(?:[ \t]+|$)/;
const setextHeading = /^( {0,3})(=+|-+)[ \t]*$/;
const wordBullet = /^(\s*)([•▪◦‣⁃])(?:[ \t\u00a0]+)(.*)$/;
const dashBullet = /^(\s*)([–—])(?:[ \t\u00a0]+)(.*)$/;
const markdownBullet = /^\s*(?:[-+*]|\d+[.)])(?:[ \t\u00a0]+)/;
const genderAsterisk = /(?<=\p{L})\*(?=\p{L})/gu;
const literalTokenTypes = new Set([
  "autolink",
  "codeFenced",
  "codeIndented",
  "codeText",
  "definitionDestination",
  "definitionLabel",
  "htmlFlow",
  "htmlText",
  "literalAutolink",
  "reference",
  "resourceDestination",
]);

function isLiteralToken(token) {
  return (
    literalTokenTypes.has(token.type) ||
    (token.type === "undefinedReference" &&
      token.parent?.type === "undefinedReferenceFull")
  );
}

function contentLines(lines, tokens, includeIndentedCode = false) {
  const fencedCode = new Set();
  const indentedCode = new Set();
  const htmlBlocks = new Set();

  for (const token of tokens) {
    const target =
      token.type === "fence"
        ? fencedCode
        : token.type === "code_block"
          ? indentedCode
          : token.type === "html_block"
            ? htmlBlocks
            : undefined;
    if (!target || !token.map) continue;

    for (let index = token.map[0]; index < token.map[1]; index += 1)
      target.add(index);
  }

  return lines.map((line, index) => {
    if (typeof line !== "string") return undefined;
    if (
      fencedCode.has(index) ||
      htmlBlocks.has(index) ||
      (!includeIndentedCode && indentedCode.has(index))
    )
      return undefined;
    return { line, index, indentedCode: indentedCode.has(index) };
  });
}

function tokenRanges(tokens, lines, predicate) {
  const ranges = new Map();

  function visit(token) {
    if (predicate(token)) {
      for (let line = token.startLine; line <= token.endLine; line += 1) {
        const start = line === token.startLine ? token.startColumn : 1;
        const end =
          line === token.endLine ? token.endColumn : lines[line - 1].length + 1;
        ranges.set(line, [...(ranges.get(line) ?? []), [start, end]]);
      }
      return;
    }

    token.children.forEach(visit);
  }

  tokens.forEach(visit);
  return ranges;
}

function isInRanges(ranges, line, column) {
  return ranges
    .get(line)
    ?.some(([start, end]) => column >= start && column < end);
}

const headingLevel = {
  names: ["MOC001", "mocogi-heading-level"],
  description:
    "User headings below generated module sections start at level three",
  tags: ["headings", "mocogi"],
  parser: "markdownit",
  function: (params, onError) => {
    const tokens = params.parsers.markdownit.tokens;
    const lines = params.lines;
    const headings = tokens.filter(
      (token) => token.type === "heading_open" && token.map,
    );
    const sections = headings.filter((token) =>
      generatedHeading.test(lines[token.map[0]]),
    );

    for (let section = 0; section < sections.length; section += 1) {
      const start = sections[section].map[1];
      const end = sections[section + 1]?.map[0] ?? lines.length;
      const sectionHeadings = headings
        .filter((token) => token.map[0] >= start && token.map[0] < end)
        .flatMap((token) => {
          const level = Number(token.tag.slice(1));

          if (token.markup.startsWith("#")) {
            const index = token.map[0];
            const match = lines[index].match(atxHeading);
            return match ? [{ index, line: lines[index], level, match }] : [];
          }

          if (["=", "-"].includes(token.markup)) {
            const titleIndex = token.map[0];
            const index = token.map[1] - 1;
            const underline = lines[index].match(setextHeading);
            return underline
              ? [
                  {
                    index,
                    line: lines[index],
                    level,
                    title: { index: titleIndex, line: lines[titleIndex] },
                    underline,
                  },
                ]
              : [];
          }

          return [];
        });

      const minimum = Math.min(...sectionHeadings.map((entry) => entry.level));
      const shift = Math.max(0, 3 - minimum);
      if (!shift) continue;

      for (const entry of sectionHeadings) {
        const newMarker = "#".repeat(Math.min(6, entry.level + shift));

        if (entry.underline) {
          onError({
            lineNumber: entry.title.index + 1,
            detail: `Expected user heading level ${newMarker.length} or deeper`,
            context: entry.title.line,
            fixInfo: { deleteCount: -1 },
          });
          onError({
            lineNumber: entry.index + 1,
            detail: `Expected user heading level ${newMarker.length} or deeper`,
            context: entry.line,
            fixInfo: {
              deleteCount: entry.line.length,
              insertText: `${entry.underline[1]}${newMarker} ${entry.title.line.trim()}`,
            },
          });
          continue;
        }

        const oldMarker = entry.match[2];
        onError({
          lineNumber: entry.index + 1,
          detail: `Expected user heading level ${newMarker.length} or deeper`,
          context: entry.line,
          range: [entry.match[1].length + 1, oldMarker.length],
          fixInfo: {
            editColumn: entry.match[1].length + 1,
            deleteCount: oldMarker.length,
            insertText: newMarker,
          },
        });
      }
    }
  },
};

const wordListMarker = {
  names: ["MOC002", "mocogi-word-list-marker"],
  description: "Word-style list markers use Markdown list syntax",
  tags: ["bullet", "mocogi"],
  parser: "markdownit",
  function: (params, onError) => {
    const lines = contentLines(
      params.lines,
      params.parsers.markdownit.tokens,
      true,
    );

    for (const entry of lines) {
      if (!entry) continue;

      const match = entry.line.match(wordBullet);
      const dashMatch = entry.line.match(dashBullet);
      const adjacentWord =
        match &&
        [lines[entry.index - 1], lines[entry.index + 1]].some(
          (other) => other?.line.match(wordBullet)?.[1] === match[1],
        );
      const adjacentDash =
        dashMatch &&
        [lines[entry.index - 1], lines[entry.index + 1]].some(
          (other) => other?.line.match(dashBullet)?.[1] === dashMatch[1],
        );
      const bullet = match ?? (adjacentDash ? dashMatch : undefined);
      if (!bullet || (entry.indentedCode && !adjacentWord && !adjacentDash))
        continue;

      const prefixLength = bullet[0].length - bullet[3].length;
      const indentation = entry.indentedCode
        ? bullet[1].replaceAll("\t", "  ").slice(0, 2)
        : bullet[1];
      onError({
        lineNumber: entry.index + 1,
        detail: `Expected Markdown list marker instead of ${bullet[2]}`,
        context: entry.line,
        range: [1, prefixLength],
        fixInfo: {
          editColumn: 1,
          deleteCount: prefixLength,
          insertText: `${indentation}- `,
        },
      });

      const next = lines[entry.index + 1];
      if (
        next?.line &&
        !next.line.match(wordBullet) &&
        !next.line.match(dashBullet) &&
        !next.line.match(markdownBullet)
      ) {
        onError({
          lineNumber: entry.index + 1,
          detail: "Expected a blank line after the pasted list",
          context: entry.line,
          fixInfo: {
            lineNumber: next.index + 1,
            editColumn: 1,
            deleteCount: 0,
            insertText: "\n",
          },
        });
      }
    }
  },
};

const escapedGenderAsterisk = {
  names: ["MOC003", "mocogi-escaped-gender-asterisk"],
  description: "Gender asterisks inside words are escaped",
  tags: ["emphasis", "gendering", "mocogi"],
  parser: "micromark",
  function: (params, onError) => {
    const candidates = params.lines.flatMap((line, index) =>
      [...line.matchAll(genderAsterisk)].map((match) => ({
        line,
        lineNumber: index + 1,
        column: match.index + 1,
      })),
    );
    const excluded = tokenRanges(
      params.parsers.micromark.tokens,
      params.lines,
      isLiteralToken,
    );

    candidates.forEach(({ line, lineNumber, column }) => {
      if (isInRanges(excluded, lineNumber, column)) return;

      onError({
        lineNumber,
        detail: "Expected an escaped gender asterisk",
        context: line,
        range: [column, 1],
        fixInfo: {
          editColumn: column,
          deleteCount: 0,
          insertText: "\\",
        },
      });
    });
  },
};

export default [headingLevel, wordListMarker, escapedGenderAsterisk];
