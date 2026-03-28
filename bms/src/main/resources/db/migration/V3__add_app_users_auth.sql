create table if not exists app_users (
    id uuid primary key,
    email varchar(180) not null unique,
    role varchar(20) not null,
    status varchar(20) not null,
    supabase_user_id varchar(100),
    invited_at timestamp not null,
    activated_at timestamp,
    is_deleted boolean not null default false,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index if not exists idx_app_users_email on app_users(email);
create index if not exists idx_app_users_role_status on app_users(role, status);
