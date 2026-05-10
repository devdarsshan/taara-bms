-- Add section field to Yarn, Spinning Orders and Spinning Deliveries
ALTER TABLE yarn_orders ADD COLUMN stitching_section_id UUID;
ALTER TABLE yarn_orders ADD CONSTRAINT fk_yarn_stitching_section FOREIGN KEY (stitching_section_id) REFERENCES stitching_sections(id);

ALTER TABLE spinning_orders ADD COLUMN stitching_section_id UUID;
ALTER TABLE spinning_orders ADD CONSTRAINT fk_spinning_order_stitching_section FOREIGN KEY (stitching_section_id) REFERENCES stitching_sections(id);

ALTER TABLE spinning_deliveries ADD COLUMN stitching_section_id UUID;
ALTER TABLE spinning_deliveries ADD CONSTRAINT fk_spinning_delivery_stitching_section FOREIGN KEY (stitching_section_id) REFERENCES stitching_sections(id);

-- Add financials to Spinning Deliveries
ALTER TABLE spinning_deliveries ADD COLUMN price_per_kg DECIMAL(12, 2);
ALTER TABLE spinning_deliveries ADD COLUMN total_price DECIMAL(12, 2);
ALTER TABLE spinning_deliveries ADD COLUMN paid_amount DECIMAL(12, 2);
ALTER TABLE spinning_deliveries ADD COLUMN balance_amount DECIMAL(12, 2);
ALTER TABLE spinning_deliveries ADD COLUMN payment_status VARCHAR(50);

-- Add total price to Cuttings
ALTER TABLE cuttings ADD COLUMN total_price DECIMAL(12, 2);
