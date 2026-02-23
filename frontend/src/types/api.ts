export type HighlightRegion = {
  verse: string;
  text: string;
  x: number;
  y: number;
  width: number;
  height: number;
};

export type BibleImageRecord = {
  id: string;
  name: string;
  outputPath: string;
  imagePath: string;
  references: string[];
  highlights: HighlightRegion[];
};
