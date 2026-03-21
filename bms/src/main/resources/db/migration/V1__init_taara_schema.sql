create sequence if not exists style_auto_seq start with 1 increment by 1;
create sequence if not exists dia_auto_seq start with 1 increment by 1;
create sequence if not exists stitching_section_auto_seq start with 1 increment by 1;
create sequence if not exists yarn_order_auto_seq start with 1 increment by 1;
create sequence if not exists spinning_order_auto_seq start with 1 increment by 1;
create sequence if not exists spinning_delivery_auto_seq start with 1 increment by 1;
create sequence if not exists inhouse_delivery_auto_seq start with 1 increment by 1;
create sequence if not exists inhouse_stock_split_auto_seq start with 1 increment by 1;
create sequence if not exists cutting_auto_seq start with 1 increment by 1;
create sequence if not exists stitching_order_auto_seq start with 1 increment by 1;
create sequence if not exists stitching_delivery_auto_seq start with 1 increment by 1;

create table if not exists styles (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    style_name varchar(150) not null unique,
    colors text not null
);

create table if not exists dias (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    dia_value varchar(50) not null unique
);

create table if not exists stitching_sections (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    section_name varchar(150) not null unique,
    type varchar(30) not null
);

create table if not exists yarn_orders (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    order_date date not null,
    style_id uuid not null references styles(id),
    quantity_kgs numeric(12, 2) not null,
    supplier_notes text
);

create table if not exists spinning_orders (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    dispatch_date date not null,
    linked_yarn_order_id uuid references yarn_orders(id),
    style_id uuid not null references styles(id),
    quantity_sent_kgs numeric(12, 2) not null,
    factory_notes text,
    auto_created boolean not null default false
);

create table if not exists spinning_deliveries (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    delivery_date date not null,
    style_id uuid not null references styles(id),
    actual_quantity_kgs numeric(12, 2) not null,
    buffer_quantity_kgs numeric(12, 2) not null,
    final_quantity_kgs numeric(12, 2) not null,
    notes text
);

create table if not exists inhouse_deliveries (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    delivery_date date not null,
    spinning_delivery_id uuid not null unique references spinning_deliveries(id),
    style_id uuid not null references styles(id),
    quantity_kgs numeric(12, 2) not null,
    split_status varchar(30) not null
);

create table if not exists inhouse_stock_splits (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    inhouse_delivery_id uuid not null references inhouse_deliveries(id),
    dia_id uuid not null references dias(id),
    style_id uuid not null references styles(id),
    quantity_kgs numeric(12, 2) not null
);

create table if not exists cuttings (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    cutting_date date not null,
    dia_id uuid not null references dias(id),
    style_id uuid not null references styles(id),
    quantity_used_kgs numeric(12, 2) not null,
    output_pieces integer,
    status varchar(30) not null,
    notes text
);

create table if not exists stitching_orders (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    order_date date not null,
    stitching_section_id uuid not null references stitching_sections(id),
    style_id uuid not null references styles(id),
    pieces_ordered integer not null,
    status varchar(30) not null,
    notes text
);

create table if not exists stitching_deliveries (
    id uuid primary key,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null,
    auto_id varchar(20) not null unique,
    delivery_date date not null,
    stitching_order_id uuid not null references stitching_orders(id),
    pieces_delivered integer not null
);

create index if not exists idx_yarn_orders_style_id on yarn_orders(style_id);
create index if not exists idx_spinning_orders_style_id on spinning_orders(style_id);
create index if not exists idx_spinning_deliveries_style_id on spinning_deliveries(style_id);
create index if not exists idx_inhouse_deliveries_style_id on inhouse_deliveries(style_id);
create index if not exists idx_inhouse_stock_splits_dia_style on inhouse_stock_splits(dia_id, style_id);
create index if not exists idx_cuttings_dia_style on cuttings(dia_id, style_id);
create index if not exists idx_stitching_orders_style_section on stitching_orders(style_id, stitching_section_id);
create index if not exists idx_stitching_deliveries_order_id on stitching_deliveries(stitching_order_id);
