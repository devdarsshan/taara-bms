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

export interface StitchingSectionRef {
  id: string;
  autoId: string;
  sectionName: string;
  type: StitchingSectionType;
}

export interface OptionItem<T = string> {
  label: string;
  value: T;
}
