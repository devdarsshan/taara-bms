import { StitchingSectionRef, StyleRef } from './common.models';

export interface SpinningDashboardResponse {
  totalDispatchedToSpinning: number;
  totalReceivedFromSpinning: number;
  netPendingAtFactory: number;
}

export interface SpinningOrder {
  id: string;
  autoId: string;
  dispatchDate: string;
  linkedYarnOrderId: string | null;
  linkedYarnOrderAutoId: string | null;
  style: StyleRef;
  stitchingSection?: StitchingSectionRef;
  quantitySentKgs: number;
  factoryNotes: string | null;
  autoCreated: boolean;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SpinningDelivery {
  id: string;
  autoId: string;
  deliveryDate: string;
  style: StyleRef;
  stitchingSection?: StitchingSectionRef;
  actualQuantityKgs: number;
  bufferQuantityKgs: number;
  finalQuantityKgs: number;
  pricePerKg?: number;
  totalPrice?: number;
  paidAmount?: number;
  balanceAmount?: number;
  paymentStatus?: string;
  notes: string | null;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SpinningOrderCreateRequest {
  dispatchDate: string;
  linkedYarnOrderAutoId?: string | null;
  styleAutoId: string;
  stitchingSectionAutoId?: string;
  quantitySentKgs: number;
  factoryNotes?: string | null;
}

export interface SpinningDeliveryCreateRequest {
  deliveryDate: string;
  styleAutoId: string;
  stitchingSectionAutoId?: string;
  actualQuantityKgs: number;
  bufferQuantityKgs?: number | null;
  pricePerKg?: number;
  paidAmount?: number;
  notes?: string | null;
}
