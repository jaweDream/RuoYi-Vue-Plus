-- ----------------------------
-- 链资产模块(ruoyi-chain)表结构与菜单 —— PostgreSQL 版
-- 由 script/sql/ry_chain.sql(MySQL 8)移植;依赖 postgres_ry_vue.sql 基础数据(部门/用户/菜单)
-- ----------------------------

-- ----------------------------
-- 1、链资产配置表
-- ----------------------------
drop table if exists chain_asset;
create table chain_asset
(
    asset_id         int8         not null,
    protocol         varchar(16)  not null,
    network          varchar(64)  not null,
    asset_type       varchar(16)  not null,
    symbol           varchar(32)  not null,
    asset_name       varchar(128) default null::varchar,
    contract_address varchar(256) default null::varchar,
    decimals         int4         default 0   not null,
    status           char(1)      default '0'::bpchar not null,
    create_dept      int8         default null,
    create_by        int8         default null,
    create_time      timestamp,
    update_by        int8         default null,
    update_time      timestamp,
    remark           varchar(500) default null::varchar,
    constraint chain_asset_pk primary key (asset_id)
);

comment on table chain_asset                    is '链资产配置表';
comment on column chain_asset.asset_id          is '资产ID';
comment on column chain_asset.protocol          is '链协议（EVM/SOLANA/APTOS）';
comment on column chain_asset.network           is '网络名（chain.networks 配置键）';
comment on column chain_asset.asset_type        is '资产类型（NATIVE原生币 TOKEN合约代币）';
comment on column chain_asset.symbol            is '资产符号';
comment on column chain_asset.asset_name        is '资产名称';
comment on column chain_asset.contract_address  is '合约标识（EVM合约地址/Solana mint/Aptos coin type）';
comment on column chain_asset.decimals          is '精度（最小单位小数位数）';
comment on column chain_asset.status            is '状态（0正常 1停用）';
comment on column chain_asset.create_dept       is '创建部门';
comment on column chain_asset.create_by         is '创建者';
comment on column chain_asset.create_time       is '创建时间';
comment on column chain_asset.update_by         is '更新者';
comment on column chain_asset.update_time       is '更新时间';
comment on column chain_asset.remark            is '备注';

create index idx_chain_asset_network ON chain_asset (network);

-- ----------------------------
-- 2、链托管地址表
-- ----------------------------
drop table if exists chain_address;
create table chain_address
(
    address_id   int8         not null,
    protocol     varchar(16)  not null,
    network      varchar(64)  not null,
    address      varchar(256) not null,
    address_type char(1)      default '0'::bpchar not null,
    user_id      int8         default null,
    status       char(1)      default '0'::bpchar not null,
    create_dept  int8         default null,
    create_by    int8         default null,
    create_time  timestamp,
    update_by    int8         default null,
    update_time  timestamp,
    remark       varchar(500) default null::varchar,
    constraint chain_address_pk primary key (address_id)
);

comment on table chain_address                  is '链托管地址表';
comment on column chain_address.address_id      is '地址ID';
comment on column chain_address.protocol        is '链协议（EVM/SOLANA/APTOS）';
comment on column chain_address.network         is '网络名';
comment on column chain_address.address         is '链上地址';
comment on column chain_address.address_type    is '地址类型（0充值地址 1热钱包）';
comment on column chain_address.user_id         is '归属业务用户ID';
comment on column chain_address.status          is '状态（0正常 1停用）';
comment on column chain_address.create_dept     is '创建部门';
comment on column chain_address.create_by       is '创建者';
comment on column chain_address.create_time     is '创建时间';
comment on column chain_address.update_by       is '更新者';
comment on column chain_address.update_time     is '更新时间';
comment on column chain_address.remark          is '备注';

create unique index uk_chain_address ON chain_address (network, address);
create index idx_chain_address_type ON chain_address (network, address_type, status);

-- ----------------------------
-- 3、扫块游标表（每个网络一行）
-- ----------------------------
drop table if exists chain_scan_cursor;
create table chain_scan_cursor
(
    cursor_id      int8         not null,
    protocol       varchar(16)  not null,
    network        varchar(64)  not null,
    scanned_height int8         default 0   not null,
    status         char(1)      default '0'::bpchar not null,
    last_error     varchar(500) default ''::varchar,
    create_dept    int8         default null,
    create_by      int8         default null,
    create_time    timestamp,
    update_by      int8         default null,
    update_time    timestamp,
    constraint chain_scan_cursor_pk primary key (cursor_id)
);

comment on table chain_scan_cursor                  is '扫块游标表';
comment on column chain_scan_cursor.cursor_id       is '游标ID';
comment on column chain_scan_cursor.protocol        is '链协议（EVM/SOLANA/APTOS）';
comment on column chain_scan_cursor.network         is '网络名';
comment on column chain_scan_cursor.scanned_height  is '已扫描高度（下次从+1开始）';
comment on column chain_scan_cursor.status          is '状态（0运行 1暂停）';
comment on column chain_scan_cursor.last_error      is '最近一次失败原因（成功后清空）';
comment on column chain_scan_cursor.create_dept     is '创建部门';
comment on column chain_scan_cursor.create_by       is '创建者';
comment on column chain_scan_cursor.create_time     is '创建时间';
comment on column chain_scan_cursor.update_by       is '更新者';
comment on column chain_scan_cursor.update_time     is '更新时间';

create unique index uk_chain_scan_cursor ON chain_scan_cursor (network);

-- ----------------------------
-- 4、链充值记录表
-- 幂等键：(network, tx_hash, event_index)
-- ----------------------------
drop table if exists chain_deposit_record;
create table chain_deposit_record
(
    deposit_id   int8          not null,
    protocol     varchar(16)   not null,
    network      varchar(64)   not null,
    asset_id     int8          not null,
    address_id   int8          not null,
    tx_hash      varchar(128)  not null,
    event_index  int4          default -1  not null,
    height       int8          not null,
    block_hash   varchar(128)  default null::varchar,
    block_time   timestamp     default null,
    from_address varchar(256)  default null::varchar,
    to_address   varchar(256)  not null,
    raw_amount   numeric(65, 0) default 0  not null,
    status       char(1)       default '0'::bpchar not null,
    create_dept  int8          default null,
    create_by    int8          default null,
    create_time  timestamp,
    update_by    int8          default null,
    update_time  timestamp,
    constraint chain_deposit_record_pk primary key (deposit_id)
);

comment on table chain_deposit_record                   is '链充值记录表';
comment on column chain_deposit_record.deposit_id       is '记录ID';
comment on column chain_deposit_record.protocol         is '链协议（EVM/SOLANA/APTOS）';
comment on column chain_deposit_record.network          is '网络名';
comment on column chain_deposit_record.asset_id         is '资产ID';
comment on column chain_deposit_record.address_id       is '命中的托管地址ID';
comment on column chain_deposit_record.tx_hash          is '交易哈希';
comment on column chain_deposit_record.event_index      is '交易内事件序号（原生币转账为-1）';
comment on column chain_deposit_record.height           is '区块高度';
comment on column chain_deposit_record.block_hash       is '区块哈希（重组核对依据）';
comment on column chain_deposit_record.block_time       is '区块时间';
comment on column chain_deposit_record.from_address     is '转出地址';
comment on column chain_deposit_record.to_address       is '转入地址（托管地址）';
comment on column chain_deposit_record.raw_amount       is '金额（最小单位原始值）';
comment on column chain_deposit_record.status           is '状态（0已确认 1已上账）';
comment on column chain_deposit_record.create_dept      is '创建部门';
comment on column chain_deposit_record.create_by        is '创建者';
comment on column chain_deposit_record.create_time      is '创建时间';
comment on column chain_deposit_record.update_by        is '更新者';
comment on column chain_deposit_record.update_time      is '更新时间';

create unique index uk_chain_deposit ON chain_deposit_record (network, tx_hash, event_index);
create index idx_chain_deposit_addr ON chain_deposit_record (network, to_address);
create index idx_chain_deposit_height ON chain_deposit_record (network, height);

-- ----------------------------
-- 5、链转账任务表
-- 业务幂等键：biz_no
-- ----------------------------
drop table if exists chain_transfer_task;
create table chain_transfer_task
(
    task_id       int8          not null,
    biz_no        varchar(64)   not null,
    protocol      varchar(16)   not null,
    network       varchar(64)   not null,
    asset_id      int8          not null,
    from_address  varchar(256)  not null,
    to_address    varchar(256)  not null,
    raw_amount    numeric(65, 0) not null,
    memo          varchar(256)  default null::varchar,
    status        char(1)       default '0'::bpchar not null,
    tx_hash       varchar(128)  default null::varchar,
    raw_fee       numeric(65, 0) default null,
    attempt_count int4          default 0   not null,
    last_error    varchar(500)  default ''::varchar,
    create_dept   int8          default null,
    create_by     int8          default null,
    create_time   timestamp,
    update_by     int8          default null,
    update_time   timestamp,
    remark        varchar(500)  default null::varchar,
    constraint chain_transfer_task_pk primary key (task_id)
);

comment on table chain_transfer_task                    is '链转账任务表';
comment on column chain_transfer_task.task_id           is '任务ID';
comment on column chain_transfer_task.biz_no            is '业务单号（上游幂等键）';
comment on column chain_transfer_task.protocol          is '链协议（EVM/SOLANA/APTOS）';
comment on column chain_transfer_task.network           is '网络名';
comment on column chain_transfer_task.asset_id          is '资产ID';
comment on column chain_transfer_task.from_address      is '出账地址（热钱包）';
comment on column chain_transfer_task.to_address        is '入账地址';
comment on column chain_transfer_task.raw_amount        is '金额（最小单位原始值）';
comment on column chain_transfer_task.memo              is '备注/附言（链支持时写入）';
comment on column chain_transfer_task.status            is '状态（0待执行 1执行中 2已广播 3成功 4失败）';
comment on column chain_transfer_task.tx_hash           is '交易哈希（广播成功后回填）';
comment on column chain_transfer_task.raw_fee           is '估算手续费（原生币最小单位）';
comment on column chain_transfer_task.attempt_count     is '已执行次数';
comment on column chain_transfer_task.last_error        is '最近一次失败原因';
comment on column chain_transfer_task.create_dept       is '创建部门';
comment on column chain_transfer_task.create_by         is '创建者';
comment on column chain_transfer_task.create_time       is '创建时间';
comment on column chain_transfer_task.update_by         is '更新者';
comment on column chain_transfer_task.update_time       is '更新时间';
comment on column chain_transfer_task.remark            is '备注';

create unique index uk_chain_transfer_bizno ON chain_transfer_task (biz_no);
create index idx_chain_transfer_status ON chain_transfer_task (status);
create index idx_chain_transfer_network ON chain_transfer_task (network);

-- ----------------------------
-- 6、菜单数据（可重跑：先清理本模块菜单 ID 区段）
-- ----------------------------
delete from sys_menu where menu_id between 1762000000000000001 and 1762000000000000018;

-- 一级目录:链资产
insert into sys_menu values(1762000000000000001, '链资产', 0, 6, 'chain', null, '', 'N', 'Y', 'M', '0', '0', '', 'money', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '链资产目录');

-- 二级菜单:资产配置
insert into sys_menu values(1762000000000000002, '资产配置', 1762000000000000001, 1, 'asset', 'chain/asset/index', '', 'N', 'Y', 'C', '0', '0', 'chain:asset:list', 'dict', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '链资产配置菜单');
insert into sys_menu values(1762000000000000003, '资产查询', 1762000000000000002, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:asset:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');
insert into sys_menu values(1762000000000000004, '资产新增', 1762000000000000002, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:asset:add', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');
insert into sys_menu values(1762000000000000005, '资产修改', 1762000000000000002, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:asset:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');
insert into sys_menu values(1762000000000000006, '资产删除', 1762000000000000002, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:asset:remove', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');

-- 二级菜单:托管地址
insert into sys_menu values(1762000000000000007, '托管地址', 1762000000000000001, 2, 'address', 'chain/address/index', '', 'N', 'Y', 'C', '0', '0', 'chain:address:list', 'guide', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '链托管地址菜单');
insert into sys_menu values(1762000000000000008, '地址查询', 1762000000000000007, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:address:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');
insert into sys_menu values(1762000000000000009, '地址新增', 1762000000000000007, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:address:add', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');
insert into sys_menu values(1762000000000000010, '地址修改', 1762000000000000007, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:address:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');
insert into sys_menu values(1762000000000000011, '地址删除', 1762000000000000007, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:address:remove', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');

-- 二级菜单:扫块监控
insert into sys_menu values(1762000000000000012, '扫块监控', 1762000000000000001, 3, 'scan', 'chain/scan/index', '', 'N', 'Y', 'C', '0', '0', 'chain:scan:list', 'monitor', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '链扫块监控菜单');
insert into sys_menu values(1762000000000000013, '游标运维', 1762000000000000012, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:scan:edit', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');
insert into sys_menu values(1762000000000000014, '充值记录', 1762000000000000012, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:deposit:list', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');

-- 二级菜单:转账任务
insert into sys_menu values(1762000000000000015, '转账任务', 1762000000000000001, 4, 'transfer', 'chain/transfer/index', '', 'N', 'Y', 'C', '0', '0', 'chain:transfer:list', 'log', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '链转账任务菜单');
insert into sys_menu values(1762000000000000016, '任务详情', 1762000000000000015, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:transfer:query', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');
insert into sys_menu values(1762000000000000017, '任务建单', 1762000000000000015, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:transfer:add', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');
insert into sys_menu values(1762000000000000018, '任务执行', 1762000000000000015, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:transfer:execute', '#', '', '', 1761000000000000103, 1761100000000000001, now(), null, null, '');
