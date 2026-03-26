drop table if exists packing_entries cascade;
drop table if exists printing_deliveries cascade;
drop table if exists printing_orders cascade;
drop table if exists stitching_delivery_allocations cascade;
drop table if exists stitching_deliveries cascade;
drop table if exists stitching_order_rows cascade;
drop table if exists stitching_orders cascade;
drop table if exists cutting_rows cascade;
drop table if exists cuttings cascade;
drop table if exists inhouse_stock_splits cascade;
drop table if exists inhouse_deliveries cascade;
drop table if exists spinning_deliveries cascade;
drop table if exists spinning_orders cascade;
drop table if exists yarn_orders cascade;
drop table if exists stitching_sections cascade;
drop table if exists dias cascade;
drop table if exists styles cascade;

drop sequence if exists style_auto_seq cascade;
drop sequence if exists dia_auto_seq cascade;
drop sequence if exists stitching_section_auto_seq cascade;
drop sequence if exists yarn_order_auto_seq cascade;
drop sequence if exists spinning_order_auto_seq cascade;
drop sequence if exists spinning_delivery_auto_seq cascade;
drop sequence if exists inhouse_delivery_auto_seq cascade;
drop sequence if exists inhouse_stock_split_auto_seq cascade;
drop sequence if exists cutting_auto_seq cascade;
drop sequence if exists stitching_order_auto_seq cascade;
drop sequence if exists stitching_delivery_auto_seq cascade;
drop sequence if exists printing_order_auto_seq cascade;
drop sequence if exists printing_delivery_auto_seq cascade;
drop sequence if exists packing_auto_seq cascade;

create sequence style_auto_seq start with 1 increment by 1;
create sequence dia_auto_seq start with 1 increment by 1;
create sequence stitching_section_auto_seq start with 1 increment by 1;
create sequence yarn_order_auto_seq start with 1 increment by 1;
create sequence spinning_order_auto_seq start with 1 increment by 1;
create sequence spinning_delivery_auto_seq start with 1 increment by 1;
create sequence inhouse_delivery_auto_seq start with 1 increment by 1;
create sequence inhouse_stock_split_auto_seq start with 1 increment by 1;
create sequence cutting_auto_seq start with 1 increment by 1;
create sequence stitching_order_auto_seq start with 1 increment by 1;
create sequence stitching_delivery_auto_seq start with 1 increment by 1;
create sequence printing_order_auto_seq start with 1 increment by 1;
create sequence printing_delivery_auto_seq start with 1 increment by 1;
create sequence packing_auto_seq start with 1 increment by 1;

create table styles (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    style_name varchar(150) not null unique,
    colors text,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table dias (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    dia_value varchar(100) not null unique,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table stitching_sections (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    section_name varchar(150) not null unique,
    partner_type varchar(30) not null,
    process_type varchar(30) not null,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table yarn_orders (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    order_date date not null,
    style_id uuid not null references styles(id),
    quantity_kgs numeric(12,2) not null,
    supplier_notes text,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table spinning_orders (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    dispatch_date date not null,
    style_id uuid not null references styles(id),
    linked_yarn_order_id uuid references yarn_orders(id),
    quantity_sent_kgs numeric(12,2) not null,
    auto_created boolean not null default false,
    factory_notes text,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table spinning_deliveries (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    delivery_date date not null,
    style_id uuid not null references styles(id),
    actual_quantity_kgs numeric(12,2) not null,
    buffer_quantity_kgs numeric(12,2) not null,
    final_quantity_kgs numeric(12,2) not null,
    notes text,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table inhouse_deliveries (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    delivery_date date not null,
    spinning_delivery_id uuid not null references spinning_deliveries(id),
    style_id uuid not null references styles(id),
    quantity_kgs numeric(12,2) not null,
    split_status varchar(30) not null,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table inhouse_stock_splits (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    delivery_id uuid not null references inhouse_deliveries(id),
    dia_id uuid not null references dias(id),
    style_id uuid not null references styles(id),
    quantity_kgs numeric(12,2) not null,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table cuttings (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    cutting_date date not null,
    total_quantity_used_kgs numeric(12,2) not null,
    total_output_pieces integer not null,
    pcs_per_kg numeric(12,2),
    status varchar(30) not null,
    notes text,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table cutting_rows (
    id uuid primary key,
    cutting_entry_id uuid not null references cuttings(id) on delete cascade,
    dia_id uuid not null references dias(id),
    style_id uuid not null references styles(id),
    size varchar(10) not null,
    quantity_used_kgs numeric(12,2) not null,
    output_pieces integer
);

create table stitching_orders (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    order_date date not null,
    expected_size varchar(10) not null,
    expected_pieces integer not null,
    status varchar(30) not null,
    notes text,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table stitching_order_rows (
    id uuid primary key,
    stitching_order_id uuid not null references stitching_orders(id) on delete cascade,
    stitching_section_id uuid not null references stitching_sections(id),
    style_id uuid not null references styles(id),
    size varchar(10) not null,
    pieces_taken integer not null
);

create table stitching_deliveries (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    delivery_date date not null,
    stitching_section_id uuid not null references stitching_sections(id),
    style_id uuid not null references styles(id),
    size varchar(10) not null,
    pieces_delivered integer not null,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table stitching_delivery_allocations (
    id uuid primary key,
    stitching_delivery_id uuid not null references stitching_deliveries(id) on delete cascade,
    stitching_order_id uuid not null references stitching_orders(id),
    allocated_pieces integer not null
);

create table printing_orders (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    order_date date not null,
    printing_section_id uuid not null references stitching_sections(id),
    style_id uuid not null references styles(id),
    size varchar(10) not null,
    pieces_ordered integer not null,
    status varchar(30) not null,
    notes text,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table printing_deliveries (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    delivery_date date not null,
    printing_order_id uuid not null references printing_orders(id),
    pieces_delivered integer not null,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table packing_entries (
    id uuid primary key,
    auto_id varchar(20) not null unique,
    packing_date date not null,
    style_id uuid not null references styles(id),
    size varchar(10) not null,
    stock_type varchar(20) not null,
    correctly_packed_pieces integer not null,
    defective_pieces integer not null,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);
