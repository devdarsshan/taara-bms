export interface MasterDashboardResponse {
  totalStyles: number;
  totalDiaTypes: number;
  totalStitchingSections: number;
}

export interface Style {
  id: string;
  autoId: string;
  styleName: string;
  colors: string[];
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Dia {
  id: string;
  autoId: string;
  diaValue: string;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export type StitchingSectionType = 'INTERNAL' | 'EXTERNAL';

export interface StitchingSection {
  id: string;
  autoId: string;
  sectionName: string;
  type: StitchingSectionType;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StyleUpsertRequest {
  styleName: string;
  colors: string[];
}

export interface DiaUpsertRequest {
  diaValue: string;
}

export interface StitchingSectionUpsertRequest {
  sectionName: string;
  type: StitchingSectionType;
}
