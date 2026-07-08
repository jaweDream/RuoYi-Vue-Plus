-- ----------------------------
-- 链资产模块(ruoyi-chain)表结构与菜单
-- 适用 MySQL 8,依赖 ry_vue.sql 基础数据(部门/用户/菜单)
-- ----------------------------

-- ----------------------------
-- 1、链资产配置表
-- ----------------------------
drop table if exists chain_asset;
create table chain_asset (
    asset_id          bigint(20)      not null                   comment '资产ID',
    protocol          varchar(16)     not null                   comment '链协议（EVM/SOLANA/APTOS）',
    network           varchar(64)     not null                   comment '网络名（chain.networks 配置键）',
    asset_type        varchar(16)     not null                   comment '资产类型（NATIVE原生币 TOKEN合约代币）',
    symbol            varchar(32)     not null                   comment '资产符号',
    asset_name        varchar(128)    default null               comment '资产名称',
    contract_address  varchar(256)    default null               comment '合约标识（EVM合约地址/Solana mint/Aptos coin type）',
    decimals          int(4)          not null default 0         comment '精度（最小单位小数位数）',
    status            char(1)         not null default '0'       comment '状态（0正常 1停用）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (asset_id),
    key idx_chain_asset_network (network)
) engine=innodb comment = '链资产配置表';

-- ----------------------------
-- 2、链托管地址表
-- ----------------------------
drop table if exists chain_address;
create table chain_address (
    address_id        bigint(20)      not null                   comment '地址ID',
    protocol          varchar(16)     not null                   comment '链协议（EVM/SOLANA/APTOS）',
    network           varchar(64)     not null                   comment '网络名',
    address           varchar(256)    not null                   comment '链上地址',
    address_type      char(1)         not null default '0'       comment '地址类型（0充值地址 1热钱包）',
    user_id           bigint(20)      default null               comment '归属业务用户ID',
    status            char(1)         not null default '0'       comment '状态（0正常 1停用）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (address_id),
    unique key uk_chain_address (network, address),
    key idx_chain_address_type (network, address_type, status)
) engine=innodb comment = '链托管地址表';

-- ----------------------------
-- 3、扫块游标表（每个网络一行）
-- ----------------------------
drop table if exists chain_scan_cursor;
create table chain_scan_cursor (
    cursor_id         bigint(20)      not null                   comment '游标ID',
    protocol          varchar(16)     not null                   comment '链协议（EVM/SOLANA/APTOS）',
    network           varchar(64)     not null                   comment '网络名',
    scanned_height    bigint(20)      not null default 0         comment '已扫描高度（下次从+1开始）',
    status            char(1)         not null default '0'       comment '状态（0运行 1暂停）',
    last_error        varchar(500)    default ''                 comment '最近一次失败原因（成功后清空）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (cursor_id),
    unique key uk_chain_scan_cursor (network)
) engine=innodb comment = '扫块游标表';

-- ----------------------------
-- 4、链充值记录表
-- 幂等键：(network, tx_hash, event_index)
-- ----------------------------
drop table if exists chain_deposit_record;
create table chain_deposit_record (
    deposit_id        bigint(20)      not null                   comment '记录ID',
    protocol          varchar(16)     not null                   comment '链协议（EVM/SOLANA/APTOS）',
    network           varchar(64)     not null                   comment '网络名',
    asset_id          bigint(20)      not null                   comment '资产ID',
    address_id        bigint(20)      not null                   comment '命中的托管地址ID',
    tx_hash           varchar(128)    not null                   comment '交易哈希',
    event_index       int(11)         not null default -1        comment '交易内事件序号（原生币转账为-1）',
    height            bigint(20)      not null                   comment '区块高度',
    block_hash        varchar(128)    default null               comment '区块哈希（重组核对依据）',
    block_time        datetime        default null               comment '区块时间',
    from_address      varchar(256)    default null               comment '转出地址',
    to_address        varchar(256)    not null                   comment '转入地址（托管地址）',
    raw_amount        decimal(65,0)   not null default 0         comment '金额（最小单位原始值）',
    status            char(1)         not null default '0'       comment '状态（0已确认 1已上账）',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (deposit_id),
    unique key uk_chain_deposit (network, tx_hash, event_index),
    key idx_chain_deposit_addr (network, to_address(64)),
    key idx_chain_deposit_height (network, height)
) engine=innodb comment = '链充值记录表';

-- ----------------------------
-- 5、链转账任务表
-- 业务幂等键：biz_no
-- ----------------------------
drop table if exists chain_transfer_task;
create table chain_transfer_task (
    task_id           bigint(20)      not null                   comment '任务ID',
    biz_no            varchar(64)     not null                   comment '业务单号（上游幂等键）',
    protocol          varchar(16)     not null                   comment '链协议（EVM/SOLANA/APTOS）',
    network           varchar(64)     not null                   comment '网络名',
    asset_id          bigint(20)      not null                   comment '资产ID',
    from_address      varchar(256)    not null                   comment '出账地址（热钱包）',
    to_address        varchar(256)    not null                   comment '入账地址',
    raw_amount        decimal(65,0)   not null                   comment '金额（最小单位原始值）',
    memo              varchar(256)    default null               comment '备注/附言（链支持时写入）',
    status            char(1)         not null default '0'       comment '状态（0待执行 1执行中 2已广播 3成功 4失败）',
    tx_hash           varchar(128)    default null               comment '交易哈希（广播成功后回填）',
    raw_fee           decimal(65,0)   default null               comment '估算手续费（原生币最小单位）',
    attempt_count     int(11)         not null default 0         comment '已执行次数',
    last_error        varchar(500)    default ''                 comment '最近一次失败原因',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)    default null               comment '备注',
    primary key (task_id),
    unique key uk_chain_transfer_bizno (biz_no),
    key idx_chain_transfer_status (status),
    key idx_chain_transfer_network (network)
) engine=innodb comment = '链转账任务表';

-- ----------------------------
-- 6、菜单数据
-- ----------------------------
-- 一级目录:链资产
insert into sys_menu values(1762000000000000001, '链资产', 0, 6, 'chain', null, '', 'N', 'Y', 'M', '0', '0', '', 'money', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '链资产目录');

-- 二级菜单:资产配置
insert into sys_menu values(1762000000000000002, '资产配置', 1762000000000000001, 1, 'asset', 'chain/asset/index', '', 'N', 'Y', 'C', '0', '0', 'chain:asset:list', 'dict', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '链资产配置菜单');
insert into sys_menu values(1762000000000000003, '资产查询', 1762000000000000002, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:asset:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1762000000000000004, '资产新增', 1762000000000000002, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:asset:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1762000000000000005, '资产修改', 1762000000000000002, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:asset:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1762000000000000006, '资产删除', 1762000000000000002, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:asset:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 二级菜单:托管地址
insert into sys_menu values(1762000000000000007, '托管地址', 1762000000000000001, 2, 'address', 'chain/address/index', '', 'N', 'Y', 'C', '0', '0', 'chain:address:list', 'guide', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '链托管地址菜单');
insert into sys_menu values(1762000000000000008, '地址查询', 1762000000000000007, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:address:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1762000000000000009, '地址新增', 1762000000000000007, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:address:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1762000000000000010, '地址修改', 1762000000000000007, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:address:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1762000000000000011, '地址删除', 1762000000000000007, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:address:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 二级菜单:扫块监控
insert into sys_menu values(1762000000000000012, '扫块监控', 1762000000000000001, 3, 'scan', 'chain/scan/index', '', 'N', 'Y', 'C', '0', '0', 'chain:scan:list', 'monitor', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '链扫块监控菜单');
insert into sys_menu values(1762000000000000013, '游标运维', 1762000000000000012, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:scan:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1762000000000000014, '充值记录', 1762000000000000012, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:deposit:list', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 二级菜单:转账任务
insert into sys_menu values(1762000000000000015, '转账任务', 1762000000000000001, 4, 'transfer', 'chain/transfer/index', '', 'N', 'Y', 'C', '0', '0', 'chain:transfer:list', 'log', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '链转账任务菜单');
insert into sys_menu values(1762000000000000016, '任务详情', 1762000000000000015, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:transfer:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1762000000000000017, '任务建单', 1762000000000000015, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:transfer:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1762000000000000018, '任务执行', 1762000000000000015, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'chain:transfer:execute', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
