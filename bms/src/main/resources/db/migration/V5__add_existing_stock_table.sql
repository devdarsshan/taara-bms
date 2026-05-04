CREATE TABLE in_house_existing_stocks (
    id UUID PRIMARY KEY,
    auto_id VARCHAR(20) NOT NULL UNIQUE,
    entry_date DATE NOT NULL,
    dia_id UUID NOT NULL REFERENCES dias(id),
    style_id UUID NOT NULL REFERENCES styles(id),
    quantity_kgs NUMERIC(12,2) NOT NULL,
    notes TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
