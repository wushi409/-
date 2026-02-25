-- run_all_data 相关 SQL，先保证语句正确，后面再做性能优化。
-- =====================================================
-- 旅游定制平台 - 数据增强汇总执行脚本
-- 
-- 将以下4个文件的内容按顺序执行：
-- 1. data_batch1_attractions.sql - 景点数据
-- 2. data_batch2_hotels_transport.sql - 酒店和交通数据  
-- 3. data_batch3_products_users.sql - 产品、用户、行为数据
-- 4. data_batch4_more.sql - 更多目的地、轮播图等
--
-- 执行方式：
-- 方式1：在 MySQL Workbench 中打开此文件并执行
-- 方式2：命令行执行 mysql -u root -p travel_db < run_all_data.sql
-- 方式3：分批导入各个SQL文件
-- =====================================================

-- 检查数据库
USE travel_db;

-- 显示当前数据统计（执行前）
SELECT '===== 执行前数据统计 =====' AS info;
SELECT 'destination' AS table_name, COUNT(*) AS count FROM destination
UNION ALL SELECT 'attraction', COUNT(*) FROM attraction
UNION ALL SELECT 'hotel', COUNT(*) FROM hotel
UNION ALL SELECT 'transport', COUNT(*) FROM transport
UNION ALL SELECT 'travel_product', COUNT(*) FROM travel_product
UNION ALL SELECT 'sys_user', COUNT(*) FROM sys_user
UNION ALL SELECT 'user_behavior', COUNT(*) FROM user_behavior
UNION ALL SELECT 'review', COUNT(*) FROM review
UNION ALL SELECT 'banner', COUNT(*) FROM banner;

-- =====================================================
-- 请依次执行以下SQL文件：
-- source data_batch1_attractions.sql;
-- source data_batch2_hotels_transport.sql;
-- source data_batch3_products_users.sql;
-- source data_batch4_more.sql;
-- =====================================================

-- 或者你可以直接复制各文件内容粘贴到MySQL中执行

SELECT '请分别执行 data_batch1 到 data_batch4 的SQL文件！' AS message;
