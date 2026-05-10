import { StitchingSectionRef, StyleRef } from './common.models';

export interface YarnDashboardResponse {
  totalYarnOrdered: number;
  yarnInOrder: number;
  yarnDispatchedToSpinning: number;
}

export interface YarnOrder {
  id: string;
  autoId: string;
  orderDate: string;
  style: StyleRef;
  quantityKgs: number;
  stitchingSection?: StitchingSectionRef;
  supplierNotes: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface YarnOrderCreateRequest {
  orderDate: string;
  styleAutoId: string;
  stitchingSectionAutoId?: string;
  quantityKgs: number;
  supplierNotes?: string | null;
}

export interface YarnOrderUpdateRequest {
  orderDate: string;
  styleAutoId: string;
  stitchingSectionAutoId?: string;
  quantityKgs: number;
  supplierNotes?: string | null;
}
