-- 사용자 테이블
create table users
(
    id                 bigint primary key not null auto_increment,
    username           varchar(50)        not null,
    password           varchar(255)       not null,
    role               varchar(20)        not null,
    created_date       timestamp default now(),
    last_modified_date timestamp,
    deleted_at         timestamp
);

-- 활성 사용자(deleted_at IS NULL)에 대해서만 username 중복을 막는 부분 유니크 인덱스
-- 소프트 삭제된 사용자의 username은 재사용 가능
create unique index uk_users_username_active on users(username) where deleted_at is null;

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

-- @SQLRestriction("deleted_at IS NULL") + created_date 정렬을 커버하는 복합 인덱스
create index idx_contents_deleted_created on contents(deleted_at, created_date desc);
