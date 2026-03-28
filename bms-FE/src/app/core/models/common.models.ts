export interface StyleRef {
  id: string;
  autoId: string;
  styleName: string;
}

export interface DiaRef {
  id: string;
  autoId: string;
  diaValue: string;
}

export type StitchingSectionType = 'INTERNAL' | 'EXTERNAL';
export type SectionProcessType = 'STITCHING' | 'PRINTING';
export type GarmentSize = 'XS' | 'S' | 'M' | 'L' | 'XL';

export interface StitchingSectionRef {
  id: string;
  autoId: string;
  sectionName: string;
  type: StitchingSectionType;
  processType: SectionProcessType;
}

export interface PieceAvailability {
  styleAutoId: string;
  size: GarmentSize;
  availablePieces: number;
}

export interface OptionItem<T = string> {
  label: string;
  value: T;
}
