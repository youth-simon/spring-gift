insert into members (email, name, kakao_id, created_at, updated_at)
values ('admin@example.com', '관리자', null, current_timestamp, current_timestamp);
insert into members (email, name, kakao_id, created_at, updated_at)
values ('user1@example.com', '카카오유저1', 1000001, current_timestamp, current_timestamp);
insert into members (email, name, kakao_id, created_at, updated_at)
values ('user2@example.com', '카카오유저2', 1000002, current_timestamp, current_timestamp);

insert into products (name, price, image_url, stock, created_at, updated_at)
values ('맥북 프로 16인치', 3360000, 'https://example.com/images/macbook.jpg', 10, current_timestamp, current_timestamp);
insert into products (name, price, image_url, stock, created_at, updated_at)
values ('아이폰 16', 1350000, 'https://example.com/images/iphone.jpg', 30, current_timestamp, current_timestamp);
insert into products (name, price, image_url, stock, created_at, updated_at)
values ('나이키 에어맥스', 179000, 'https://example.com/images/airmax.jpg', 15, current_timestamp, current_timestamp);
insert into products (name, price, image_url, stock, created_at, updated_at)
values ('레비스 청바지', 89000, 'https://example.com/images/jeans.jpg', 25, current_timestamp, current_timestamp);
insert into products (name, price, image_url, stock, created_at, updated_at)
values ('제주 감귤 5kg', 25000, 'https://example.com/images/tangerine.jpg', 50, current_timestamp, current_timestamp);
insert into products (name, price, image_url, stock, created_at, updated_at)
values ('한우 등심 1kg', 65000, 'https://example.com/images/beef.jpg', 8, current_timestamp, current_timestamp);

insert into wishes (member_id, product_id, quantity, created_at, updated_at)
values (2, 1, 1, current_timestamp, current_timestamp);
insert into wishes (member_id, product_id, quantity, created_at, updated_at)
values (2, 3, 2, current_timestamp, current_timestamp);
insert into wishes (member_id, product_id, quantity, created_at, updated_at)
values (3, 2, 1, current_timestamp, current_timestamp);
insert into wishes (member_id, product_id, quantity, created_at, updated_at)
values (3, 5, 3, current_timestamp, current_timestamp);
