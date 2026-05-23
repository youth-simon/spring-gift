create table members
(
    id         bigint auto_increment primary key,
    email      varchar(100) not null,
    name       varchar(50)  not null,
    kakao_id   bigint,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    constraint uk_members_email unique (email),
    constraint uk_members_kakao_id unique (kakao_id)
);
create index idx_members_created_at on members (created_at desc);

create table products
(
    id         bigint auto_increment primary key,
    name       varchar(50)   not null,
    price      bigint        not null,
    image_url  varchar(1000) not null,
    stock      int           not null,
    created_at datetime(6)   not null,
    updated_at datetime(6)   not null
);
create index idx_products_name on products (name);

create table wishes
(
    id         bigint auto_increment primary key,
    member_id  bigint      not null,
    product_id bigint      not null,
    quantity   int         not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint uk_wishes_member_product unique (member_id, product_id),
    constraint fk_wishes_member foreign key (member_id) references members (id),
    constraint fk_wishes_product foreign key (product_id) references products (id)
);
create index idx_wishes_member_created on wishes (member_id, created_at desc);
create index idx_wishes_product on wishes (product_id);

create table wish_activities
(
    id          bigint auto_increment primary key,
    member_id   bigint      not null,
    product_id  bigint      not null,
    type        varchar(32) not null,
    quantity    int,
    occurred_at datetime(6) not null,
    created_at  datetime(6) not null,
    updated_at  datetime(6) not null
);
create index idx_wish_activities_member_occurred on wish_activities (member_id, occurred_at desc);
create index idx_wish_activities_product_occurred on wish_activities (product_id, occurred_at desc);
