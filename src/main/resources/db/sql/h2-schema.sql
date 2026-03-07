-- 사용자 테이블
create table users
(
    id                 bigint primary key not null auto_increment,
    username           varchar(50)        not null,
    password           varchar(255)       not null,
    role               varchar(20)        not null,
    created_date       timestamp default now(),
    last_modified_date timestamp,
    deleted_at         timestamp,
    constraint uk_users_username unique (username)
);

-- 콘텐츠 테이블
create table contents
(
    id                 bigint primary key not null auto_increment,
    title              varchar(100)       not null,
    description        text,
    view_count         bigint             not null default 0,
    created_date       timestamp,
    created_by         varchar(50)        not null,
    last_modified_date timestamp,
    last_modified_by   varchar(50),
    deleted_at         timestamp
);
