ALTER TABLE stitching_order_rows ADD COLUMN rate_per_piece numeric(12,2);
ALTER TABLE cutting_rows ADD COLUMN rate_per_piece numeric(12,2);
ALTER TABLE cutting_rows DROP COLUMN output_pieces;
