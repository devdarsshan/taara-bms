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
export type SectionProcessType = 'STITCHING' | 'PRINTING' | 'KNITTING';
export type GarmentSize = 'XS' | 'S' | 'M' | 'L' | 'XL' | '2XL';

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

export interface ReadyToStitchBreakdownResponse {
  size: GarmentSize;
  totalPieces: number;
}

export interface OptionItem<T = string> {
  label: string;
  value: T;
}
