-- Schema only, with inline foreign keys for ER tools
CREATE DATABASE IF NOT EXISTS travel_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travel_db;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(50),
  avatar VARCHAR(255),
  phone VARCHAR(20),
  email VARCHAR(100),
  role TINYINT NOT NULL COMMENT '0-admin 1-user 2-provider',
  status TINYINT DEFAULT 1 COMMENT '0-disabled 1-enabled',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS destination (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  province VARCHAR(50),
  city VARCHAR(50),
  description TEXT,
  cover_image VARCHAR(255),
  images TEXT COMMENT 'JSON array',
  longitude DECIMAL(10,6),
  latitude DECIMAL(10,6),
  hot_score INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS provider_qualification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  company_name VARCHAR(100),
  license_no VARCHAR(100),
  license_image VARCHAR(255),
  contact_person VARCHAR(50),
  contact_phone VARCHAR(20),
  audit_status TINYINT DEFAULT 0 COMMENT '0-pending 1-approved 2-rejected',
  audit_remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_provider_qualification_user
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS attraction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  destination_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  cover_image VARCHAR(255),
  ticket_price DECIMAL(10,2),
  open_time VARCHAR(100),
  address VARCHAR(255),
  longitude DECIMAL(10,6),
  latitude DECIMAL(10,6),
  tags VARCHAR(255) COMMENT 'comma separated tags',
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_attraction_destination
    FOREIGN KEY (destination_id) REFERENCES destination(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hotel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  destination_id BIGINT NOT NULL,
  provider_id BIGINT,
  name VARCHAR(100) NOT NULL,
  star_level TINYINT,
  description TEXT,
  cover_image VARCHAR(255),
  address VARCHAR(255),
  longitude DECIMAL(10,6),
  latitude DECIMAL(10,6),
  price_min DECIMAL(10,2),
  price_max DECIMAL(10,2),
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_hotel_destination
    FOREIGN KEY (destination_id) REFERENCES destination(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_hotel_provider
    FOREIGN KEY (provider_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transport (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type TINYINT COMMENT '0-flight 1-train 2-bus 3-car-rental',
  departure VARCHAR(100),
  arrival VARCHAR(100),
  price DECIMAL(10,2),
  description TEXT,
  provider_id BIGINT,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_transport_provider
    FOREIGN KEY (provider_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  provider_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  cover_image VARCHAR(255),
  images TEXT,
  destination_id BIGINT,
  duration INT COMMENT 'days',
  price DECIMAL(10,2),
  original_price DECIMAL(10,2),
  product_type TINYINT COMMENT '0-group 1-free 2-local-guide 3-user-route',
  tags VARCHAR(255),
  include_items TEXT,
  exclude_items TEXT,
  stock INT DEFAULT 999,
  sales INT DEFAULT 0,
  status TINYINT DEFAULT 1 COMMENT '0-offline 1-online',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_travel_product_provider
    FOREIGN KEY (provider_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_travel_product_destination
    FOREIGN KEY (destination_id) REFERENCES destination(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_daily_stock (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  stock_date DATE NOT NULL,
  stock_total INT NOT NULL DEFAULT 0,
  stock_available INT NOT NULL DEFAULT 0,
  warn_threshold INT DEFAULT 0,
  status TINYINT DEFAULT 1 COMMENT '0-disabled 1-enabled',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_product_date (product_id, stock_date),
  INDEX idx_product (product_id),
  INDEX idx_stock_date (stock_date),
  CONSTRAINT fk_product_daily_stock_product
    FOREIGN KEY (product_id) REFERENCES travel_product(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_day_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  day_number INT NOT NULL,
  title VARCHAR(200),
  description TEXT,
  attraction_ids VARCHAR(255),
  hotel_id BIGINT,
  transport_id BIGINT,
  CONSTRAINT fk_travel_day_plan_product
    FOREIGN KEY (product_id) REFERENCES travel_product(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_travel_day_plan_hotel
    FOREIGN KEY (hotel_id) REFERENCES hotel(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_travel_day_plan_transport
    FOREIGN KEY (transport_id) REFERENCES transport(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS custom_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider_id BIGINT,
  destination_id BIGINT,
  title VARCHAR(200),
  budget_min DECIMAL(10,2),
  budget_max DECIMAL(10,2),
  start_date DATE,
  end_date DATE,
  people_count INT,
  preferences TEXT,
  interest_tags VARCHAR(255),
  status TINYINT DEFAULT 0 COMMENT '0-pending 1-planned 2-accepted 3-cancelled',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_custom_request_user
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_custom_request_provider
    FOREIGN KEY (provider_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_custom_request_destination
    FOREIGN KEY (destination_id) REFERENCES destination(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS custom_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  provider_id BIGINT NOT NULL,
  title VARCHAR(200),
  description TEXT,
  total_price DECIMAL(10,2),
  day_plans TEXT COMMENT 'daily plan JSON',
  status TINYINT DEFAULT 0 COMMENT '0-pending 1-accepted 2-rejected',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_custom_plan_request
    FOREIGN KEY (request_id) REFERENCES custom_request(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_custom_plan_provider
    FOREIGN KEY (provider_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) UNIQUE NOT NULL,
  user_id BIGINT NOT NULL,
  provider_id BIGINT NOT NULL,
  product_id BIGINT,
  custom_plan_id BIGINT,
  order_type TINYINT COMMENT '0-product-order 1-custom-order',
  total_amount DECIMAL(10,2),
  status TINYINT DEFAULT 0 COMMENT '0-unpaid 1-paid 2-progress 3-finished 4-cancelled 5-refunding 6-refunded',
  people_count INT,
  travel_date DATE,
  contact_name VARCHAR(50),
  contact_phone VARCHAR(20),
  remark TEXT,
  pay_time DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_travel_order_user
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_travel_order_provider
    FOREIGN KEY (provider_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_travel_order_product
    FOREIGN KEY (product_id) REFERENCES travel_product(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_travel_order_custom_plan
    FOREIGN KEY (custom_plan_id) REFERENCES custom_plan(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  msg_type TINYINT DEFAULT 0 COMMENT '0-text 1-image',
  is_read TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_chat_message_sender
    FOREIGN KEY (sender_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_chat_message_receiver
    FOREIGN KEY (receiver_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider_id BIGINT NOT NULL,
  last_message TEXT,
  last_time DATETIME,
  unread_count INT DEFAULT 0,
  CONSTRAINT fk_chat_session_user
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_chat_session_provider
    FOREIGN KEY (provider_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_behavior (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  behavior_type TINYINT COMMENT '0-view 1-favorite 2-buy 3-review',
  score DECIMAL(3,1),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id),
  INDEX idx_product (product_id),
  CONSTRAINT fk_user_behavior_user
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_user_behavior_product
    FOREIGN KEY (product_id) REFERENCES travel_product(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product (user_id, product_id),
  CONSTRAINT fk_user_favorite_user
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_user_favorite_product
    FOREIGN KEY (product_id) REFERENCES travel_product(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  order_id BIGINT,
  rating TINYINT COMMENT '1-5',
  content TEXT,
  images TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_user
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_review_product
    FOREIGN KEY (product_id) REFERENCES travel_product(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_review_order
    FOREIGN KEY (order_id) REFERENCES travel_order(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS travel_note (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_id BIGINT,
  title VARCHAR(120) NOT NULL,
  destination_name VARCHAR(100) NOT NULL,
  travel_date DATE,
  content TEXT NOT NULL,
  tags TEXT COMMENT 'JSON array',
  images TEXT COMMENT 'JSON array',
  rating TINYINT DEFAULT 5 COMMENT '1-5',
  status TINYINT DEFAULT 1 COMMENT '0-hidden 1-published',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_travel_note_user (user_id),
  INDEX idx_travel_note_order (order_id),
  INDEX idx_travel_note_create_time (create_time),
  CONSTRAINT fk_travel_note_user
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_travel_note_order
    FOREIGN KEY (order_id) REFERENCES travel_order(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_change_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  order_no VARCHAR(50) NOT NULL,
  product_title VARCHAR(200),
  provider_id BIGINT NOT NULL,
  provider_name VARCHAR(50),
  user_id BIGINT NOT NULL,
  user_name VARCHAR(50),
  expected_date DATE,
  reason VARCHAR(500) NOT NULL,
  status TINYINT DEFAULT 0 COMMENT '0-pending 1-approved 2-rejected',
  review_remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME,
  INDEX idx_change_request_order (order_id),
  INDEX idx_change_request_provider (provider_id),
  INDEX idx_change_request_user (user_id),
  CONSTRAINT fk_change_request_order
    FOREIGN KEY (order_id) REFERENCES travel_order(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_change_request_provider
    FOREIGN KEY (provider_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_change_request_user
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100),
  image_url VARCHAR(255),
  link_url VARCHAR(255),
  sort_order INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== Seed Data =====
INSERT INTO sys_user (username, password, nickname, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 0, 1);

-- 测试游客账号 (密码: 123456)
INSERT INTO sys_user (username, password, nickname, phone, role, status) VALUES
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', '13800138001', 1, 1),
('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', '13800138002', 1, 1),
('user3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', '13800138003', 1, 1);

-- 测试服务商账号 (密码: 123456)
INSERT INTO sys_user (username, password, nickname, phone, role, status) VALUES
('provider1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '阳光旅行社', '13900139001', 2, 1),
('provider2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '环球旅游', '13900139002', 2, 1);

-- 目的地数据
INSERT INTO destination (name, province, city, description, hot_score, longitude, latitude) VALUES
('三亚', '海南', '三亚市', '三亚位于海南岛最南端，是中国最南部的热带滨海旅游城市，拥有美丽的海滩和热带风光。', 95, 109.508268, 18.247872),
('丽江', '云南', '丽江市', '丽江古城是中国历史文化名城，以纳西族文化和古城风貌闻名于世。', 90, 100.233026, 26.872108),
('杭州', '浙江', '杭州市', '杭州是中国著名的旅游城市，以西湖风景区闻名，素有"人间天堂"之称。', 88, 120.153576, 30.287459),
('成都', '四川', '成都市', '成都是中国西南地区的中心城市，以美食、大熊猫和悠闲的生活方式著称。', 85, 104.065735, 30.659462),
('北京', '北京', '北京市', '中国首都，拥有故宫、长城等众多世界文化遗产。', 92, 116.405285, 39.904989),
('西安', '陕西', '西安市', '十三朝古都，拥有兵马俑、大雁塔等历史文化遗迹。', 82, 108.948024, 34.263161),
('桂林', '广西', '桂林市', '桂林山水甲天下，以喀斯特地貌和漓江风光闻名。', 80, 110.299121, 25.274215),
('厦门', '福建', '厦门市', '美丽的海滨城市，鼓浪屿是著名的旅游景点。', 78, 118.089425, 24.479834);

-- 景点数据
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, tags) VALUES
(1, '亚龙湾', '亚龙湾是三亚最美的海湾之一，被誉为"天下第一湾"。', 0, '全天开放', '海滩,免费,自然风光'),
(1, '天涯海角', '天涯海角是三亚著名的旅游景点，象征着爱情的永恒。', 81, '07:30-18:30', '文化,海景,地标'),
(1, '南山文化旅游区', '南山寺是中国最南端的佛教文化旅游区。', 129, '08:00-17:30', '文化,宗教,景区'),
(2, '丽江古城', '世界文化遗产，纳西族古城风貌保存完好。', 0, '全天开放', '古城,文化,免费'),
(2, '玉龙雪山', '纳西族神山，海拔5596米，终年积雪。', 100, '07:00-16:00', '雪山,自然,户外'),
(3, '西湖', '中国十大风景名胜之一，世界文化遗产。', 0, '全天开放', '湖泊,免费,文化'),
(3, '灵隐寺', '杭州最著名的佛教寺院，始建于东晋。', 75, '07:00-18:00', '寺庙,文化,宗教'),
(4, '大熊猫繁育研究基地', '世界上最大的大熊猫繁育研究机构。', 55, '07:30-18:00', '动物,亲子,自然'),
(4, '宽窄巷子', '成都最具代表性的历史文化街区。', 0, '全天开放', '美食,文化,免费'),
(5, '故宫博物院', '中国明清两代的皇家宫殿，世界文化遗产。', 60, '08:30-17:00', '历史,文化,世界遗产'),
(5, '长城（八达岭）', '世界七大奇迹之一，中国古代伟大的防御工程。', 40, '06:30-19:00', '历史,户外,世界遗产');

-- 酒店数据
INSERT INTO hotel (destination_id, name, star_level, description, price_min, price_max) VALUES
(1, '三亚亚龙湾万豪度假酒店', 5, '位于亚龙湾核心区域，享有私人海滩。', 800, 3000),
(1, '三亚海棠湾民宿', 3, '温馨舒适的海边民宿，性价比高。', 200, 500),
(2, '丽江古城花间堂', 4, '位于古城内的精品酒店，纳西风格。', 400, 1200),
(3, '杭州西湖国宾馆', 5, '坐落于西湖畔的顶级酒店。', 1000, 5000),
(4, '成都太古里博舍酒店', 5, '位于太古里商圈的设计酒店。', 600, 2000),
(5, '北京王府井大饭店', 5, '位于王府井商业区的豪华酒店。', 500, 2500);

-- 旅游产品数据
INSERT INTO travel_product (provider_id, title, description, destination_id, duration, price, original_price, product_type, tags, include_items, exclude_items, stock, sales) VALUES
(5, '三亚5天4晚浪漫海岛游', '畅游三亚湾、亚龙湾、天涯海角，体验热带海岛风情。', 1, 5, 3999, 4999, 0, '海岛,浪漫,度假', '往返机票,酒店住宿,景点门票,导游服务', '个人消费,旅游保险', 50, 128),
(5, '丽江大理6天5晚深度游', '探索丽江古城、玉龙雪山、大理洱海，感受云南风情。', 2, 6, 4599, 5599, 0, '古城,雪山,文化', '往返机票,酒店住宿,景点门票', '个人消费', 30, 86),
(5, '杭州西湖3天2晚自由行', '自由探索西湖美景，品味杭州美食文化。', 3, 3, 1999, 2499, 1, '自由行,美食,文化', '酒店住宿,接送机', '机票,餐饮,门票', 100, 215),
(6, '成都美食文化4天3晚', '品尝地道川菜，游览大熊猫基地和宽窄巷子。', 4, 4, 2899, 3499, 0, '美食,文化,亲子', '酒店住宿,景点门票,美食体验', '往返交通', 40, 156),
(6, '北京经典5日游', '游览故宫、长城、天坛等经典景点。', 5, 5, 3599, 4299, 0, '历史,文化,经典', '酒店住宿,景点门票,导游服务', '往返交通,个人消费', 60, 342),
(6, '西安历史文化3日游', '探访兵马俑、大雁塔、回民街。', 6, 3, 1899, 2299, 0, '历史,文化,美食', '酒店住宿,景点门票', '往返交通', 45, 98);

-- 用户行为数据（用于协同过滤推荐）
INSERT INTO user_behavior (user_id, product_id, behavior_type) VALUES
(2, 1, 0), (2, 1, 1), (2, 3, 0), (2, 5, 0), (2, 5, 2),
(3, 1, 0), (3, 2, 0), (3, 2, 1), (3, 4, 0), (3, 4, 2),
(4, 3, 0), (4, 3, 1), (4, 5, 0), (4, 6, 0), (4, 6, 2),
(2, 4, 0), (3, 5, 0), (4, 1, 0), (4, 2, 0);

-- 轮播图
INSERT INTO banner (title, image_url, link_url, sort_order, status) VALUES
('三亚热带海岛游', '/uploads/banner/sanya.jpg', '/product/1', 1, 1),
('丽江古城深度游', '/uploads/banner/lijiang.jpg', '/product/2', 2, 1),
('杭州西湖自由行', '/uploads/banner/hangzhou.jpg', '/product/3', 3, 1);


-- ========================
-- NEXT FILE
-- ========================

-- =====================================================
-- 旅游定制平台 - 增强测试数据
-- 分批执行，确保数据丰富度
-- =====================================================

USE travel_db;

-- =====================================================
-- 第1批：补充三亚景点（核心目的地，确保行程生成丰富）
-- =====================================================

INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
-- 三亚（destination_id = 1）更多景点
(1, '蜈支洲岛', '被誉为"中国的马尔代夫"，拥有清澈的海水和丰富的水上活动项目。', 144, '08:00-17:30', '三亚市海棠区蜈支洲岛', '海岛,潜水,浪漫,海滩'),
(1, '亚特兰蒂斯水世界', '三亚最大的水上乐园，拥有多种刺激的水上滑道和冲浪池。', 338, '10:00-18:00', '三亚市海棠区海棠北路', '水上乐园,亲子,娱乐'),
(1, '鹿回头公园', '三亚标志性景点，可俯瞰三亚湾全景，日落时分尤为壮观。', 45, '07:30-22:00', '三亚市吉阳区鹿回头路', '观景,日落,地标,文化'),
(1, '大东海', '三亚最早开发的海滨浴场，沙滩细腻，适合游泳和日光浴。', 0, '全天开放', '三亚市吉阳区大东海旅游区', '海滩,免费,游泳,日光浴'),
(1, '第一市场', '三亚最大的海鲜市场，可以购买新鲜海鲜加工品尝。', 0, '06:00-22:00', '三亚市天涯区新建街155号', '美食,海鲜,购物,地道'),
(1, '椰梦长廊', '全长20公里的海滨大道，是欣赏日落和椰林风光的绝佳地点。', 0, '全天开放', '三亚市天涯区三亚湾路', '免费,日落,骑行,海滩'),
(1, '槟榔谷黎苗文化旅游区', '了解海南黎族和苗族传统文化的最佳去处。', 120, '08:00-17:30', '三亚市吉阳区槟榔谷', '文化,民俗,体验'),
(1, '呀诺达热带雨林', '热带雨林生态旅游区，可体验丛林探险和高空滑索。', 170, '07:30-18:00', '三亚市保亭县三道镇', '自然,探险,雨林,户外'),
(1, '西岛', '距离三亚市区最近的海岛，有潜水、海钓等多种海上项目。', 148, '08:00-17:30', '三亚市天涯区西岛', '海岛,潜水,海钓,休闲'),
(1, '南山大小洞天', '海南道教文化旅游区，有1400年历史的洞天福地。', 75, '08:00-18:00', '三亚市崖州区南山村', '道教,文化,历史,自然');

-- =====================================================
-- 第2批：补充其他热门目的地景点
-- =====================================================

-- 丽江（destination_id = 2）更多景点
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(2, '束河古镇', '比丽江古城更安静的纳西古镇，保留着原始的古镇风貌。', 30, '全天开放', '丽江市古城区束河街道', '古镇,文化,安静,摄影'),
(2, '拉市海', '丽江最大的高原湖泊，可以骑马划船，观鸟天堂。', 30, '08:00-18:00', '丽江市玉龙县拉市乡', '湖泊,骑马,观鸟,自然'),
(2, '蓝月谷', '位于玉龙雪山脚下，湖水呈蓝色，像一轮蓝月亮。', 0, '08:00-17:00', '丽江市玉龙县玉龙雪山景区内', '湖泊,自然,摄影,雪山'),
(2, '白沙古镇', '丽江最古老的古镇，有著名的白沙壁画。', 0, '全天开放', '丽江市玉龙县白沙乡', '古镇,壁画,文化,历史'),
(2, '黑龙潭公园', '丽江城市公园，可以拍摄玉龙雪山倒影。', 0, '07:00-20:00', '丽江市古城区玉泉路', '公园,免费,雪山,摄影'),
(2, '泸沽湖', '云南与四川交界的高原湖泊，摩梭族聚居地。', 70, '全天开放', '丽江市宁蒗县泸沽湖', '湖泊,民族,自然,浪漫');

-- 杭州（destination_id = 3）更多景点
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(3, '雷峰塔', '西湖十景之一，白娘子传说的发生地。', 40, '08:00-20:30', '杭州市西湖区南山路15号', '塔,传说,西湖,文化'),
(3, '西溪湿地', '国家湿地公园，《非诚勿扰》取景地。', 60, '08:00-17:30', '杭州市西湖区天目山路518号', '湿地,自然,电影取景,生态'),
(3, '千岛湖', '国家5A级景区，拥有1078个岛屿的人工湖。', 150, '08:00-17:00', '杭州市淳安县千岛湖镇', '湖泊,岛屿,自然,游船'),
(3, '宋城', '大型宋代主题公园，《宋城千古情》演出闻名。', 310, '09:00-21:00', '杭州市西湖区之江路148号', '主题公园,演出,文化,历史'),
(3, '六和塔', '钱塘江畔古塔，登塔可观钱塘江大桥。', 20, '06:30-17:30', '杭州市西湖区之江路16号', '古塔,历史,江景,文化'),
(3, '河坊街', '杭州历史文化街区，美食购物一条街。', 0, '全天开放', '杭州市上城区河坊街', '美食,购物,文化,免费');

-- 成都（destination_id = 4）更多景点
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(4, '锦里古街', '成都最古老的商业街之一，体验地道成都生活。', 0, '09:00-22:00', '成都市武侯区武侯祠大街231号', '古街,美食,文化,购物'),
(4, '武侯祠', '中国唯一的君臣合祀祠庙，三国文化圣地。', 60, '08:00-18:00', '成都市武侯区武侯祠大街231号', '历史,三国,文化,祠堂'),
(4, '杜甫草堂', '唐代诗人杜甫流寓成都时的故居。', 50, '08:00-18:30', '成都市青羊区青华路37号', '历史,诗人,园林,文化'),
(4, '青城山', '中国道教名山，世界文化遗产。', 80, '08:00-18:00', '成都市都江堰市青城山镇', '道教,山岳,自然,世界遗产'),
(4, '都江堰', '世界水利工程奇迹，两千年仍在使用。', 80, '08:00-18:00', '成都市都江堰市公园路', '水利,历史,世界遗产,工程'),
(4, '春熙路', '成都最繁华的商业步行街，购物天堂。', 0, '全天开放', '成都市锦江区春熙路', '购物,时尚,美食,免费'),
(4, '人民公园', '成都人喝茶聊天的休闲胜地，体验成都慢生活。', 0, '06:00-22:00', '成都市青羊区少城路12号', '公园,茶馆,休闲,免费');

-- 北京（destination_id = 5）更多景点
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(5, '天坛公园', '明清皇帝祭天的场所，世界文化遗产。', 15, '06:00-22:00', '北京市东城区天坛路甲1号', '皇家,历史,世界遗产,园林'),
(5, '颐和园', '皇家园林博物馆，中国四大名园之一。', 30, '06:30-18:00', '北京市海淀区新建宫门路19号', '皇家园林,历史,世界遗产,湖泊'),
(5, '圆明园', '万园之园遗址，见证历史沧桑。', 10, '07:00-19:00', '北京市海淀区清华西路28号', '历史,遗址,园林,教育'),
(5, '天安门广场', '世界上最大的城市广场，国家象征。', 0, '05:00-22:00', '北京市东城区东长安街', '地标,广场,免费,历史'),
(5, '王府井步行街', '北京最著名的商业街，百年老字号汇聚。', 0, '全天开放', '北京市东城区王府井大街', '购物,美食,老字号,免费'),
(5, '南锣鼓巷', '北京最古老的街区之一，胡同文化代表。', 0, '全天开放', '北京市东城区南锣鼓巷', '胡同,文化,购物,免费'),
(5, '798艺术区', '当代艺术聚集区，文艺青年打卡地。', 0, '全天开放', '北京市朝阳区酒仙桥路4号', '艺术,文化,创意,免费');

-- 西安（destination_id = 6）更多景点
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(6, '兵马俑', '世界第八大奇迹，秦始皇陵的陪葬坑。', 120, '08:30-17:00', '西安市临潼区秦陵北路', '历史,世界遗产,考古,兵马俑'),
(6, '大雁塔', '唐代著名佛塔，玄奘法师译经之所。', 50, '08:00-17:30', '西安市雁塔区大慈恩寺内', '佛塔,历史,唐代,文化'),
(6, '回民街', '西安最著名的美食街，各种清真小吃。', 0, '全天开放', '西安市莲湖区北院门街', '美食,清真,购物,免费'),
(6, '西安城墙', '中国保存最完整的古代城墙。', 54, '08:00-22:00', '西安市碑林区南门', '城墙,历史,骑行,古建筑'),
(6, '华清宫', '唐代皇家温泉宫殿，杨贵妃沐浴之所。', 120, '07:00-18:00', '西安市临潼区华清路38号', '皇家,温泉,历史,唐代'),
(6, '大唐芙蓉园', '全方位展示盛唐风貌的大型皇家园林。', 120, '09:00-21:00', '西安市雁塔区芙蓉西路99号', '唐代,园林,演出,夜景'),
(6, '永兴坊', '非遗美食街区，品尝陕西各地特色美食。', 0, '全天开放', '西安市新城区东新街', '美食,非遗,文化,免费');

-- 桂林（destination_id = 7）景点
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(7, '漓江', '桂林山水甲天下的精华所在，乘竹筏游览最佳。', 216, '08:00-17:00', '桂林市阳朔县', '山水,游船,自然,摄影'),
(7, '阳朔西街', '中西文化交融的旅游步行街，酒吧美食云集。', 0, '全天开放', '桂林市阳朔县西街', '购物,美食,酒吧,免费'),
(7, '象鼻山', '桂林城徽，形似大象汲水的奇特山形。', 55, '06:00-22:00', '桂林市象山区滨江路', '地标,山峰,摄影,自然'),
(7, '两江四湖', '桂林市区环城水系，夜游尤为精彩。', 190, '08:00-22:30', '桂林市象山区', '夜游,湖泊,城市,浪漫'),
(7, '龙脊梯田', '世界梯田之冠，壮观的农耕奇迹。', 80, '08:00-18:00', '桂林市龙胜县龙脊镇', '梯田,自然,摄影,民族'),
(7, '银子岩', '桂林最美溶洞，钟乳石晶莹剔透如银子。', 75, '08:30-17:30', '桂林市荔浦市马岭镇', '溶洞,自然,地质,奇观');

-- 厦门（destination_id = 8）景点
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(8, '鼓浪屿', '万国建筑博览，钢琴之岛，世界文化遗产。', 0, '全天开放', '厦门市思明区鼓浪屿', '海岛,建筑,文化,世界遗产'),
(8, '南普陀寺', '闽南佛教圣地，始建于唐代。', 3, '04:00-18:00', '厦门市思明区思明南路515号', '寺庙,佛教,历史,免费'),
(8, '厦门大学', '中国最美大学之一，嘉庚建筑风格。', 0, '限时开放', '厦门市思明区思明南路422号', '大学,建筑,免费,文化'),
(8, '曾厝垵', '厦门最文艺的小渔村，美食民宿聚集地。', 0, '全天开放', '厦门市思明区曾厝垵', '文艺,美食,民宿,免费'),
(8, '中山路步行街', '厦门最繁华的商业街，骑楼建筑风格。', 0, '全天开放', '厦门市思明区中山路', '购物,美食,骑楼,免费'),
(8, '环岛路', '中国最美滨海大道之一，骑行观海最佳。', 0, '全天开放', '厦门市思明区环岛路', '骑行,海景,免费,日落'),
(8, '胡里山炮台', '清代海防要塞，拥有世界最大的海岸炮。', 25, '07:30-18:00', '厦门市思明区曾厝垵路2号', '历史,军事,海景,古迹');

SELECT '景点数据插入完成！' AS message;


-- ========================
-- NEXT FILE
-- ========================

-- =====================================================
-- 第2批：酒店和交通数据
-- =====================================================

USE travel_db;

-- =====================================================
-- 补充酒店数据
-- =====================================================

-- 三亚酒店（destination_id = 1）
INSERT INTO hotel (destination_id, name, star_level, description, address, price_min, price_max) VALUES
(1, '三亚艾迪逊酒店', 5, '位于海棠湾的顶级设计酒店，私人沙滩。', '三亚市海棠区海棠北路', 1500, 5000),
(1, '三亚太阳湾柏悦酒店', 5, '隐于山海之间的奢华度假酒店。', '三亚市吉阳区太阳湾路', 2000, 6000),
(1, '三亚半山半岛洲际度假酒店', 5, '高尔夫球场旁的滨海度假酒店。', '三亚市天涯区小东海', 900, 3500),
(1, '三亚蜈支洲岛珊瑚酒店', 5, '坐落于蜈支洲岛的独特海岛酒店。', '三亚市海棠区蜈支洲岛', 1800, 4500),
(1, '三亚亚龙湾红树林度假酒店', 5, '亚龙湾核心位置，东南亚风情。', '三亚市吉阳区亚龙湾', 700, 2500),
(1, '三亚海棠湾君悦酒店', 5, '现代设计与热带风情完美融合。', '三亚市海棠区海棠北路', 800, 3000),
(1, '三亚湾假日酒店', 4, '性价比高的海滨酒店。', '三亚市天涯区三亚湾路', 350, 800),
(1, '三亚大东海银泰阳光度假酒店', 4, '大东海核心区域，交通便利。', '三亚市吉阳区大东海', 400, 1000),
(1, '三亚四季海庭海景民宿', 3, '海景民宿，温馨家庭氛围。', '三亚市吉阳区半山半岛', 180, 450),
(1, '三亚椰林海景青年旅舍', 2, '背包客首选，价格实惠。', '三亚市天涯区三亚湾', 80, 200);

-- 丽江酒店（destination_id = 2）
INSERT INTO hotel (destination_id, name, star_level, description, address, price_min, price_max) VALUES
(2, '丽江悦榕庄', 5, '雪山脚下的顶级度假村。', '丽江市古城区束河古镇旁', 2500, 8000),
(2, '丽江金茂璞修雪山酒店', 5, '玉龙雪山最近的奢华酒店。', '丽江市玉龙县玉龙雪山', 3000, 10000),
(2, '丽江大研安缦酒店', 5, '古城内的极致奢华体验。', '丽江市古城区狮子山', 4000, 12000),
(2, '丽江古城英迪格酒店', 4, '古城内的精品设计酒店。', '丽江市古城区七一街', 600, 1500),
(2, '丽江束河花筑酒店', 4, '束河古镇内的花园酒店。', '丽江市古城区束河街道', 350, 800),
(2, '丽江古城驼峰客栈', 3, '古城核心位置的特色客栈。', '丽江市古城区五一街', 150, 400),
(2, '泸沽湖湖思茶屋民宿', 3, '泸沽湖畔的湖景民宿。', '丽江市宁蒗县泸沽湖', 200, 600);

-- 杭州酒店（destination_id = 3）
INSERT INTO hotel (destination_id, name, star_level, description, address, price_min, price_max) VALUES
(3, '杭州安缦法云', 5, '隐于千年古刹的极致隐世之所。', '杭州市西湖区法云弄22号', 5000, 15000),
(3, '杭州柏悦酒店', 5, '钱江新城CBD顶级商务酒店。', '杭州市上城区钱江路1366号', 1500, 4000),
(3, '杭州西湖四季酒店', 5, '西湖畔的顶级度假酒店。', '杭州市西湖区灵隐路5号', 2500, 8000),
(3, '杭州西溪悦榕庄', 5, '西溪湿地内的度假胜地。', '杭州市西湖区紫金港路21号', 2000, 5000),
(3, '杭州西湖君悦酒店', 5, '湖滨商圈核心位置。', '杭州市上城区湖滨路28号', 1200, 3500),
(3, '杭州灵隐寺旁茶禅民宿', 3, '灵隐寺旁的禅意民宿。', '杭州市西湖区灵隐路', 300, 600),
(3, '千岛湖绿城度假酒店', 5, '千岛湖畔五星级度假酒店。', '杭州市淳安县千岛湖镇', 800, 2500);

-- 成都酒店（destination_id = 4）
INSERT INTO hotel (destination_id, name, star_level, description, address, price_min, price_max) VALUES
(4, '成都钓鱼台精品酒店', 5, '国宾馆级别的奢华体验。', '成都市武侯区顺江路', 2000, 6000),
(4, '成都香格里拉大酒店', 5, '市中心顶级商务酒店。', '成都市锦江区滨江东路9号', 1000, 3000),
(4, '成都IFS瑰丽酒店', 5, '太古里商圈的时尚之选。', '成都市锦江区红星路三段1号', 1500, 4000),
(4, '成都宽窄巷子智选假日', 4, '宽窄巷子旁的便捷酒店。', '成都市青羊区下同仁路', 350, 700),
(4, '成都熊猫基地附近民宿', 3, '大熊猫基地周边民宿。', '成都市成华区熊猫大道', 200, 500),
(4, '青城山六善酒店', 5, '青城山顶级生态度假酒店。', '成都市都江堰市青城山镇', 3000, 8000);

-- 北京酒店（destination_id = 5）
INSERT INTO hotel (destination_id, name, star_level, description, address, price_min, price_max) VALUES
(5, '北京华尔道夫酒店', 5, '王府井顶级奢华酒店。', '北京市东城区金鱼胡同5-15号', 2500, 8000),
(5, '北京璞瑄酒店', 5, '王府中環的艺术设计酒店。', '北京市东城区王府井大街1号', 2000, 6000),
(5, '北京瑰丽酒店', 5, '朝阳门CBD区域顶级酒店。', '北京市朝阳区朝阳门外大街乙12号', 1800, 5000),
(5, '北京颐和安缦酒店', 5, '颐和园旁的极致隐世之所。', '北京市海淀区颐和园宫门前街1号', 4000, 12000),
(5, '北京王府井希尔顿酒店', 5, '王府井商业区便捷之选。', '北京市东城区东长安街8号', 800, 2000),
(5, '北京南锣鼓巷四合院民宿', 3, '胡同里的四合院体验。', '北京市东城区南锣鼓巷', 400, 1000),
(5, '长城脚下山居酒店', 4, '尽览长城风光的山居酒店。', '北京市延庆区八达岭镇', 600, 1500);

-- 西安酒店（destination_id = 6）
INSERT INTO hotel (destination_id, name, star_level, description, address, price_min, price_max) VALUES
(6, '西安W酒店', 5, '城墙脚下的潮流奢华酒店。', '西安市碑林区南大街199号', 1200, 3500),
(6, '西安威斯汀大酒店', 5, '高新区商务中心顶级酒店。', '西安市高新区唐延南路1号', 700, 2000),
(6, '西安索菲特人民大厦', 5, '历史悠久的标志性酒店。', '西安市新城区东新街319号', 600, 1800),
(6, '西安城墙脚下民宿', 3, '南门城墙旁的文化民宿。', '西安市碑林区南门', 250, 600),
(6, '西安回民街附近客栈', 3, '美食街旁的便捷客栈。', '西安市莲湖区北院门', 180, 450),
(6, '华清宫御汤温泉酒店', 4, '华清宫景区内温泉酒店。', '西安市临潼区华清路', 500, 1500);

-- 桂林酒店（destination_id = 7）
INSERT INTO hotel (destination_id, name, star_level, description, address, price_min, price_max) VALUES
(7, '桂林香格里拉大酒店', 5, '漓江畔五星级度假酒店。', '桂林市象山区环城南一路111号', 800, 2500),
(7, '阳朔悦榕庄', 5, '遇龙河畔的顶级度假村。', '桂林市阳朔县阳朔镇', 2500, 6000),
(7, '阳朔糖舍酒店', 4, '老糖厂改造的精品酒店。', '桂林市阳朔县阳朔镇', 600, 1500),
(7, '龙脊梯田全景民宿', 3, '梯田核心位置，风景绝佳。', '桂林市龙胜县龙脊镇', 200, 500),
(7, '阳朔西街青年旅舍', 2, '西街核心位置，背包客首选。', '桂林市阳朔县西街', 50, 150);

-- 厦门酒店（destination_id = 8）
INSERT INTO hotel (destination_id, name, star_level, description, address, price_min, price_max) VALUES
(8, '厦门安达仕酒店', 5, '双子塔顶层的奢华体验。', '厦门市思明区演武西路189号', 1500, 4000),
(8, '鼓浪屿别墅酒店', 4, '鼓浪屿岛上的历史别墅。', '厦门市思明区鼓浪屿', 800, 2500),
(8, '厦门泰禾厦门院子', 5, '闽南院落式奢华酒店。', '厦门市翔安区新店镇', 2000, 5000),
(8, '曾厝垵海景民宿', 3, '曾厝垵的海景民宿。', '厦门市思明区曾厝垵', 200, 600),
(8, '厦门鹭江宾馆', 4, '中山路商圈历史酒店。', '厦门市思明区鹭江道54号', 400, 1000),
(8, '环岛路海景青年旅舍', 2, '环岛路旁的性价比之选。', '厦门市思明区环岛路', 80, 200);

-- =====================================================
-- 交通数据
-- =====================================================

INSERT INTO transport (type, departure, arrival, price, description, status) VALUES
-- 飞机航班（type = 0）
(0, '北京', '三亚', 1200, '北京首都机场-三亚凤凰机场，约4小时', 1),
(0, '上海', '三亚', 1100, '上海浦东机场-三亚凤凰机场，约3.5小时', 1),
(0, '广州', '三亚', 650, '广州白云机场-三亚凤凰机场，约1.5小时', 1),
(0, '成都', '三亚', 900, '成都双流机场-三亚凤凰机场，约3小时', 1),
(0, '北京', '丽江', 1500, '北京首都机场-丽江三义机场，约4小时', 1),
(0, '上海', '丽江', 1350, '上海浦东机场-丽江三义机场，约3.5小时', 1),
(0, '广州', '昆明', 700, '广州白云机场-昆明长水机场，约2小时', 1),
(0, '深圳', '杭州', 550, '深圳宝安机场-杭州萧山机场，约2小时', 1),
(0, '北京', '成都', 800, '北京首都机场-成都双流机场，约3小时', 1),
(0, '上海', '西安', 600, '上海虹桥机场-西安咸阳机场，约2.5小时', 1),
(0, '北京', '桂林', 750, '北京首都机场-桂林两江机场，约3小时', 1),
(0, '上海', '厦门', 450, '上海虹桥机场-厦门高崎机场，约1.5小时', 1);

-- 火车/高铁（type = 1）
INSERT INTO transport (type, departure, arrival, price, description, status) VALUES
(1, '北京', '杭州', 538, '北京南站-杭州东站，高铁约5小时', 1),
(1, '上海', '杭州', 73, '上海虹桥-杭州东站，高铁约45分钟', 1),
(1, '北京', '西安', 515, '北京西站-西安北站，高铁约4.5小时', 1),
(1, '成都', '西安', 263, '成都东站-西安北站，高铁约3.5小时', 1),
(1, '广州', '桂林', 215, '广州南站-桂林西站，高铁约2.5小时', 1),
(1, '深圳', '厦门', 150, '深圳北站-厦门北站，高铁约3.5小时', 1),
(1, '上海', '北京', 553, '上海虹桥-北京南站，高铁约4.5小时', 1),
(1, '广州', '深圳', 75, '广州南站-深圳北站，高铁约30分钟', 1),
(1, '杭州', '苏州', 88, '杭州东站-苏州北站，高铁约1.5小时', 1),
(1, '西安', '洛阳', 175, '西安北站-洛阳龙门站，高铁约1.5小时', 1);

-- 大巴（type = 2）
INSERT INTO transport (type, departure, arrival, price, description, status) VALUES
(2, '丽江', '大理', 60, '丽江客运站-大理客运站，约3小时', 1),
(2, '昆明', '丽江', 180, '昆明西部客运站-丽江客运站，约8小时', 1),
(2, '桂林', '阳朔', 28, '桂林汽车总站-阳朔汽车站，约1.5小时', 1),
(2, '成都', '九寨沟', 150, '成都茶店子客运站-九寨沟口，约10小时', 1),
(2, '厦门', '土楼', 50, '厦门湖滨南长途汽车站-南靖土楼，约3小时', 1),
(2, '三亚', '海口', 80, '三亚汽车站-海口汽车东站，约4小时', 1);

-- 租车（type = 3）
INSERT INTO transport (type, departure, arrival, price, description, status) VALUES
(3, '三亚', '三亚周边', 200, '经济型轿车日租，含基本保险', 1),
(3, '三亚', '三亚周边', 350, 'SUV日租，适合海岛自驾游', 1),
(3, '丽江', '丽江周边', 250, '经济型轿车日租，适合古城周边自驾', 1),
(3, '成都', '川西', 400, 'SUV日租，适合川西自驾游', 1),
(3, '杭州', '杭州周边', 200, '经济型轿车日租，适合西湖周边', 1),
(3, '桂林', '桂林阳朔', 180, '经济型轿车日租，适合漓江自驾', 1),
(3, '厦门', '厦门周边', 180, '经济型轿车日租，适合环岛路自驾', 1),
(3, '西安', '西安周边', 200, '经济型轿车日租，适合周边古迹游览', 1);

SELECT '酒店和交通数据插入完成！' AS message;


-- ========================
-- NEXT FILE
-- ========================

-- =====================================================
-- 第3批：旅游产品、用户、行为数据、评价数据
-- =====================================================

USE travel_db;

-- =====================================================
-- 旅游产品数据（服务商ID使用5和6）
-- =====================================================

-- 三亚产品（destination_id = 1）
INSERT INTO travel_product (provider_id, title, description, destination_id, duration, price, original_price, product_type, tags, include_items, exclude_items, stock, sales) VALUES
(5, '三亚3天2晚海岛休闲游', '轻松惬意的短途海岛假期，畅游亚龙湾和天涯海角。', 1, 3, 1999, 2599, 1, '海岛,休闲,短途', '酒店住宿,接送机,天涯海角门票', '机票,餐饮,其他个人消费', 80, 256),
(5, '三亚蜈支洲岛深度2日游', '深度体验蜈支洲岛的潜水和海上娱乐项目。', 1, 2, 1599, 1999, 2, '潜水,海岛,深度', '蜈支洲岛门票,往返船票,酒店1晚', '潜水装备租赁费,餐饮', 50, 189),
(5, '三亚亲子7天6晚欢乐游', '带孩子畅游三亚各大主题公园和海滩。', 1, 7, 6999, 8999, 0, '亲子,主题公园,海滩', '酒店住宿,机票,水世界门票,导游服务', '个人消费,部分餐饮', 30, 95),
(6, '三亚蜜月5天4晚浪漫之旅', '专为新婚夫妇定制的浪漫海岛蜜月行程。', 1, 5, 5999, 7599, 3, '蜜月,浪漫,海岛', '海景房住宿,烛光晚餐,情侣SPA', '机票,个人消费', 40, 168),
(6, '三亚高端8天7晚私人定制', '专属管家服务，尊享三亚顶级度假体验。', 1, 8, 15999, 19999, 3, '高端,私人定制,奢华', '顶级酒店,专车接送,私人导游,米其林餐厅', '个人购物', 20, 45);

-- 丽江产品（destination_id = 2）
INSERT INTO travel_product (provider_id, title, description, destination_id, duration, price, original_price, product_type, tags, include_items, exclude_items, stock, sales) VALUES
(5, '丽江古城3天2晚慢生活游', '悠闲漫步丽江古城，感受纳西族风情。', 2, 3, 1599, 1999, 1, '古城,慢生活,文化', '古城内精品客栈,下午茶体验', '机票,餐饮,索道费', 100, 298),
(5, '丽江玉龙雪山一日游', '登顶玉龙雪山，游览蓝月谷，含氧气瓶。', 2, 1, 580, 780, 2, '雪山,一日游,自然', '门票,索道,氧气瓶,午餐,导游', '防寒服租赁', 200, 856),
(6, '丽江大理6天5晚深度游', '丽江古城+玉龙雪山+大理洱海深度体验。', 2, 6, 4599, 5599, 0, '深度,跟团,两城', '酒店住宿,大巴交通,门票,导游', '索道费,个人消费', 60, 245),
(6, '泸沽湖3天2晚摄影之旅', '摩梭族文化体验，拍摄泸沽湖绝美风光。', 2, 3, 2199, 2799, 3, '摄影,民族,湖泊', '湖景房住宿,猪槽船,摄影指导', '交通费,餐饮', 50, 178),
(5, '丽江束河古镇2天1晚休闲游', '远离喧嚣，体验更原始的纳西古镇。', 2, 2, 899, 1199, 1, '古镇,休闲,小众', '束河精品民宿,下午茶', '交通,餐饮,门票', 80, 156);

-- 杭州产品（destination_id = 3）
INSERT INTO travel_product (provider_id, title, description, destination_id, duration, price, original_price, product_type, tags, include_items, exclude_items, stock, sales) VALUES
(5, '杭州西湖2天1晚精品游', '西湖十景精华游览，品尝地道杭帮菜。', 3, 2, 1299, 1599, 1, '西湖,精品,美食', '湖景酒店,西湖游船,龙井茶体验', '机票,餐饮', 120, 456),
(5, '杭州乌镇3天2晚水乡游', '杭州西湖+乌镇水乡完美组合。', 3, 3, 1899, 2399, 0, '水乡,两地,经典', '酒店住宿,乌镇门票,往返交通', '个人消费', 80, 312),
(6, '千岛湖2天1晚亲子游', '千岛湖游船+鱼拓体验，亲子互动乐趣多。', 3, 2, 1599, 1999, 0, '亲子,湖泊,体验', '湖景酒店,游船票,鱼拓体验', '餐饮,交通', 60, 198),
(6, '杭州宋城千古情一日游', '观看宋城千古情大型演出，穿越千年。', 3, 1, 499, 650, 2, '演出,文化,一日游', '宋城门票,千古情演出票,往返接送', '餐饮', 200, 789),
(5, '杭州西溪湿地4天3晚生态游', '西溪湿地深度体验，感受城市中的自然。', 3, 4, 2599, 3199, 1, '湿地,生态,深度', '湿地酒店,门票,摇橹船,采摘体验', '交通,餐饮', 40, 123);

-- 成都产品（destination_id = 4）
INSERT INTO travel_product (provider_id, title, description, destination_id, duration, price, original_price, product_type, tags, include_items, exclude_items, stock, sales) VALUES
(6, '成都3天2晚美食文化游', '火锅、串串、担担面...吃遍成都美食。', 4, 3, 1799, 2199, 1, '美食,文化,网红', '酒店住宿,美食地图,部分餐厅代金券', '交通,大部分餐饮', 100, 567),
(6, '成都大熊猫基地半日游', '近距离观看国宝大熊猫，含早间入园。', 4, 1, 299, 399, 2, '熊猫,亲子,半日游', '门票,往返接送,讲解服务', '餐饮,纪念品', 300, 1256),
(5, '成都青城山都江堰2日游', '双世界遗产深度游览。', 4, 2, 999, 1299, 0, '世界遗产,道教,水利', '门票,酒店,导游,午餐', '索道费,个人消费', 80, 345),
(5, '川西稻城亚丁7天6晚自驾游', '成都出发，自驾穿越川西高原。', 4, 7, 5999, 7599, 3, '自驾,川西,风光', '酒店住宿,车辆租赁,保险', '油费,餐饮,门票', 30, 89),
(6, '成都周末锦里宽窄巷子1日游', '成都最具代表性的文化街区一日游。', 4, 1, 199, 299, 2, '文化,美食,周末', '导游讲解,小吃代金券', '交通,大部分餐饮', 500, 2134);

-- 北京产品（destination_id = 5）
INSERT INTO travel_product (provider_id, title, description, destination_id, duration, price, original_price, product_type, tags, include_items, exclude_items, stock, sales) VALUES
(6, '北京故宫长城3天2晚经典游', '打卡北京必游景点，感受皇城气派。', 5, 3, 2199, 2799, 0, '经典,历史,皇城', '酒店住宿,门票,导游服务', '交通,餐饮', 100, 678),
(6, '北京5天4晚深度文化游', '故宫、颐和园、天坛、长城全覆盖。', 5, 5, 3999, 4999, 0, '深度,文化,全景', '酒店住宿,景点门票,导游,部分餐饮', '往返交通', 60, 234),
(5, '北京胡同文化半日游', '坐人力三轮车游胡同，品老北京味道。', 5, 1, 299, 399, 2, '胡同,文化,体验', '三轮车游览,胡同讲解,老北京小吃', '正餐', 200, 567),
(5, '北京八达岭长城一日游', '最热门的长城段落，含往返接送。', 5, 1, 398, 498, 2, '长城,一日游,经典', '门票,往返接送,午餐', '缆车费', 300, 1456),
(5, '北京环球影城2天1晚欢乐游', '畅玩北京环球影城主题公园。', 5, 2, 1999, 2599, 0, '主题公园,亲子,娱乐', '门票,酒店住宿,快速通道券', '餐饮,纪念品', 80, 456);

-- 西安产品（destination_id = 6）
INSERT INTO travel_product (provider_id, title, description, destination_id, duration, price, original_price, product_type, tags, include_items, exclude_items, stock, sales) VALUES
(5, '西安兵马俑华清宫1日游', '秦始皇兵马俑+华清宫经典一日游。', 6, 1, 458, 598, 2, '兵马俑,历史,一日游', '门票,往返接送,午餐,讲解', '个人消费', 300, 1789),
(5, '西安3天2晚历史文化游', '兵马俑、城墙、大雁塔、回民街全体验。', 6, 3, 1899, 2399, 0, '历史,文化,美食', '酒店住宿,门票,导游', '交通,餐饮', 80, 345),
(6, '西安美食夜市美食之旅', '回民街+永兴坊，品尝陕西各地美食。', 6, 1, 199, 299, 2, '美食,夜市,地道', '美食地图,部分小吃', '餐饮费', 500, 2345),
(6, '西安大唐芙蓉园夜游', '夜游大唐芙蓉园，观看大唐追梦演出。', 6, 1, 298, 398, 2, '夜游,演出,唐代', '门票,演出票', '交通,餐饮', 200, 678),
(6, '西安兵马俑+华山3天2晚', '感受秦汉历史，挑战西岳华山。', 6, 3, 2599, 3199, 0, '历史,登山,挑战', '酒店住宿,门票,索道,导游', '餐饮,登山装备', 50, 189);

-- 桂林产品（destination_id = 7）
INSERT INTO travel_product (provider_id, title, description, destination_id, duration, price, original_price, product_type, tags, include_items, exclude_items, stock, sales) VALUES
(5, '桂林漓江阳朔4天3晚精华游', '漓江竹筏+阳朔西街+遇龙河漂流。', 7, 4, 2399, 2999, 0, '山水,精华,经典', '酒店住宿,竹筏,漂流,导游', '往返交通,餐饮', 80, 456),
(5, '桂林漓江精华一日游', '乘船游漓江，欣赏桂林山水甲天下。', 7, 1, 398, 498, 2, '漓江,一日游,山水', '船票,午餐,往返接送', '门票', 200, 987),
(6, '龙脊梯田2天1晚摄影游', '日出日落时分拍摄壮观的龙脊梯田。', 7, 2, 899, 1199, 3, '梯田,摄影,民族', '梯田门票,住宿,摄影指导', '交通,餐饮', 50, 234),
(6, '阳朔西街3天2晚休闲游', '漫步西街，品尝啤酒鱼，体验阳朔慢生活。', 7, 3, 1299, 1599, 1, '西街,休闲,美食', '江景酒店,遇龙河漂流', '交通,餐饮,其他门票', 100, 345),
(5, '桂林银子岩+芦笛岩溶洞探秘', '探索桂林神奇的喀斯特溶洞。', 7, 1, 298, 398, 2, '溶洞,地质,一日游', '门票,往返接送', '餐饮', 150, 567);

-- 厦门产品（destination_id = 8）
INSERT INTO travel_product (provider_id, title, description, destination_id, duration, price, original_price, product_type, tags, include_items, exclude_items, stock, sales) VALUES
(6, '厦门鼓浪屿3天2晚文艺游', '漫步鼓浪屿，打卡厦大，品尝闽南美食。', 8, 3, 1599, 1999, 1, '文艺,海岛,美食', '酒店住宿,鼓浪屿船票', '交通,餐饮,其他门票', 100, 567),
(6, '厦门4天3晚休闲度假游', '鼓浪屿+环岛路+曾厝垵全体验。', 8, 4, 2199, 2799, 0, '休闲,海岛,度假', '酒店住宿,门票,环岛路骑行', '往返交通,餐饮', 80, 345),
(5, '厦门土楼一日游', '探访世界文化遗产——福建土楼。', 8, 1, 298, 398, 2, '土楼,世界遗产,一日游', '门票,往返接送,午餐', '个人消费', 200, 789),
(5, '厦门2天1晚亲子海洋游', '厦门科技馆+海底世界，亲子科普之旅。', 8, 2, 999, 1299, 0, '亲子,海洋,科普', '酒店住宿,门票', '交通,餐饮', 60, 234),
(5, '金门一日游', '从厦门出发游览金门，体验两岸风情。', 8, 1, 599, 799, 2, '金门,两岸,一日游', '船票,门票,午餐', '签证办理', 100, 678);

-- =====================================================
-- 更多用户数据
-- =====================================================

INSERT INTO sys_user (username, password, nickname, phone, email, role, status) VALUES
-- 更多游客用户（role = 1）
('user4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵六', '13800138004', 'user4@test.com', 1, 1),
('user5', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '钱七', '13800138005', 'user5@test.com', 1, 1),
('user6', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '孙八', '13800138006', 'user6@test.com', 1, 1),
('user7', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周九', '13800138007', 'user7@test.com', 1, 1),
('user8', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '吴十', '13800138008', 'user8@test.com', 1, 1),
('traveler01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '旅行达人小明', '15900001001', 'xiaoming@travel.com', 1, 1),
('traveler02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '爱旅游的小红', '15900001002', 'xiaohong@travel.com', 1, 1),
('photographer', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '摄影师老王', '15900001003', 'wang@photo.com', 1, 1),
-- 更多服务商用户（role = 2）
('provider3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '海南岛旅行社', '13900139003', 'hainan@travel.com', 2, 1),
('provider4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '云南印象旅游', '13900139004', 'yunnan@travel.com', 2, 1),
('provider5', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '江南水乡游', '13900139005', 'jiangnan@travel.com', 2, 1);

-- =====================================================
-- 用户行为数据（协同过滤推荐需要）
-- =====================================================

INSERT INTO user_behavior (user_id, product_id, behavior_type, score) VALUES
-- behavior_type: 0-浏览 1-收藏 2-购买 3-评分
-- 用户7-14的行为数据
(7, 1, 0, NULL), (7, 1, 1, NULL), (7, 2, 0, NULL), (7, 7, 0, NULL), (7, 7, 2, NULL),
(8, 1, 0, NULL), (8, 3, 0, NULL), (8, 3, 1, NULL), (8, 8, 0, NULL), (8, 8, 2, NULL),
(9, 2, 0, NULL), (9, 2, 1, NULL), (9, 4, 0, NULL), (9, 9, 0, NULL), (9, 9, 2, NULL),
(10, 5, 0, NULL), (10, 5, 1, NULL), (10, 6, 0, NULL), (10, 10, 0, NULL), (10, 10, 2, NULL),
(11, 3, 0, NULL), (11, 4, 0, NULL), (11, 4, 1, NULL), (11, 11, 0, NULL), (11, 11, 2, NULL),
(12, 1, 0, NULL), (12, 2, 0, NULL), (12, 5, 0, NULL), (12, 5, 1, NULL), (12, 12, 2, NULL),
(13, 6, 0, NULL), (13, 6, 2, NULL), (13, 7, 0, NULL), (13, 8, 0, NULL),
(14, 1, 0, NULL), (14, 3, 0, NULL), (14, 5, 0, NULL), (14, 7, 0, NULL), (14, 9, 0, NULL),
-- 老用户更多行为
(2, 7, 0, NULL), (2, 8, 0, NULL), (2, 9, 0, NULL), (2, 9, 1, NULL),
(3, 10, 0, NULL), (3, 11, 0, NULL), (3, 12, 0, NULL), (3, 12, 1, NULL),
(4, 7, 0, NULL), (4, 8, 0, NULL), (4, 8, 1, NULL), (4, 13, 0, NULL);

-- 添加一些评分数据
INSERT INTO user_behavior (user_id, product_id, behavior_type, score) VALUES
(7, 7, 3, 4.5), (8, 8, 3, 5.0), (9, 9, 3, 4.0), (10, 10, 3, 4.8),
(11, 11, 3, 4.2), (12, 12, 3, 4.7), (13, 6, 3, 4.9);

-- =====================================================
-- 评价数据
-- =====================================================

INSERT INTO review (user_id, product_id, order_id, rating, content) VALUES
(2, 1, NULL, 5, '三亚之行非常完美！酒店很棒，海水清澈，导游服务专业。'),
(3, 2, NULL, 5, '丽江古城太美了，纳西族的文化让人流连忘返。'),
(4, 3, NULL, 4, '杭州西湖名不虚传，但人有点多，建议淡季去。'),
(2, 4, NULL, 5, '成都美食太赞了！熊猫基地的大熊猫超级可爱！'),
(3, 5, NULL, 5, '北京故宫震撼人心，长城壮观，值得每个中国人去一次。'),
(4, 6, NULL, 4, '西安的历史文化底蕴太深厚了，兵马俑令人叹为观止。'),
(7, 1, NULL, 5, '服务很周到，行程安排合理，下次还会选择这家旅行社！'),
(8, 2, NULL, 4, '玉龙雪山太壮观了，虽然有点累但非常值得。'),
(9, 3, NULL, 5, '西湖的夜景特别美，雷峰塔映在湖面上太浪漫了。'),
(10, 4, NULL, 5, '宽窄巷子的小吃太多了，吃到撑都吃不完！'),
(11, 5, NULL, 5, '颐和园的昆明湖太大了，走了一整天，景色超美。'),
(12, 6, NULL, 4, '回民街的羊肉泡馍正宗，就是人太多要排队。');

-- =====================================================
-- 定制需求示例数据
-- =====================================================

INSERT INTO custom_request (user_id, destination_id, title, budget_min, budget_max, start_date, end_date, people_count, preferences, interest_tags, status) VALUES
(2, 1, '三亚蜜月旅行', 8000, 15000, '2026-03-01', '2026-03-07', 2, '希望安排海景房，有浪漫的烛光晚餐', '海滩,浪漫,美食', 0),
(3, 2, '丽江亲子游', 6000, 10000, '2026-04-15', '2026-04-20', 4, '带两个小朋友，希望行程不要太累', '亲子,文化,自然', 0),
(4, 4, '成都美食之旅', 3000, 6000, '2026-05-01', '2026-05-05', 3, '主要是来吃美食的，希望多安排美食行程', '美食,文化,休闲', 0),
(7, 5, '北京历史文化游', 5000, 8000, '2026-06-01', '2026-06-05', 2, '对历史文化很感兴趣，希望有专业讲解', '历史,文化,世界遗产', 0),
(8, 7, '桂林摄影之旅', 4000, 7000, '2026-07-10', '2026-07-15', 1, '喜欢摄影，希望安排日出日落的最佳拍摄点', '摄影,山水,自然', 0);

SELECT '产品、用户和行为数据插入完成！' AS message;


-- ========================
-- NEXT FILE
-- ========================

-- =====================================================
-- 第4批：轮播图、服务商资质、更多目的地
-- =====================================================

USE travel_db;

-- =====================================================
-- 补充更多目的地
-- =====================================================

INSERT INTO destination (name, province, city, description, hot_score, longitude, latitude) VALUES
('张家界', '湖南', '张家界市', '以奇特的石英砂岩峰林地貌著称，是阿凡达取景地。', 83, 110.479191, 29.117096),
('黄山', '安徽', '黄山市', '五岳归来不看山，黄山归来不看岳。中国最著名的山岳景区。', 86, 118.337481, 29.714658),
('九寨沟', '四川', '阿坝州', '童话世界，人间仙境。以彩色海子和瀑布群闻名。', 84, 103.917442, 33.260579),
('青岛', '山东', '青岛市', '红瓦绿树、碧海蓝天，著名的海滨城市和啤酒之都。', 79, 120.382639, 36.067082),
('大连', '辽宁', '大连市', '东北之窗，浪漫之都。美丽的海滨城市。', 76, 121.614682, 38.914003),
('苏州', '江苏', '苏州市', '上有天堂，下有苏杭。以园林和水乡闻名于世。', 81, 120.585316, 31.298886),
('拉萨', '西藏', '拉萨市', '日光城，世界屋脊上的圣城。藏传佛教圣地。', 77, 91.132212, 29.660361),
('重庆', '重庆', '重庆市', '山城、雾都、火锅之都。8D魔幻立体城市。', 84, 106.551556, 29.563009);

-- =====================================================
-- 新目的地景点
-- =====================================================

-- 张家界景点（destination_id = 9）
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(9, '张家界国家森林公园', '阿凡达取景地，石英砂岩峰林奇观。', 225, '07:00-18:00', '张家界市武陵源区', '山岳,自然,世界遗产,电影取景'),
(9, '天门山', '天门洞、玻璃栈道，惊险刺激的空中之旅。', 238, '08:00-18:00', '张家界市永定区', '山岳,玻璃栈道,索道,刺激'),
(9, '大峡谷玻璃桥', '世界最长最高的全透明玻璃桥。', 219, '07:00-18:00', '张家界市慈利县', '玻璃桥,刺激,峡谷,网红'),
(9, '黄龙洞', '世界溶洞奇观，定海神针令人惊叹。', 100, '08:00-17:00', '张家界市武陵源区', '溶洞,地质,自然');

-- 黄山景点（destination_id = 10）
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(10, '黄山风景区', '中国十大名山之首，奇松怪石云海温泉。', 190, '06:00-17:30', '黄山市黄山区', '山岳,日出,云海,摄影'),
(10, '宏村', '画里乡村，中国最美的古村落之一。', 104, '07:30-17:30', '黄山市黟县', '古村,徽派建筑,摄影,文化'),
(10, '西递', '世界文化遗产，徽派建筑杰出代表。', 104, '07:30-17:30', '黄山市黟县', '古村,世界遗产,徽派,文化'),
(10, '屯溪老街', '流动的清明上河图，徽商文化发源地。', 0, '全天开放', '黄山市屯溪区', '古街,购物,美食,文化');

-- 九寨沟景点（destination_id = 11）
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(11, '九寨沟风景区', '童话世界，人间仙境。彩色海子和瀑布群。', 169, '08:30-17:00', '阿坝州九寨沟县', '湖泊,瀑布,自然,世界遗产'),
(11, '黄龙风景区', '人间瑶池，钙化池彩池群。', 170, '08:00-17:00', '阿坝州松潘县', '钙化池,自然,高原,世界遗产');

-- 青岛景点（destination_id = 12）
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(12, '栈桥', '青岛标志性景点，百年历史的海上长廊。', 0, '全天开放', '青岛市市南区', '地标,海滨,免费,历史'),
(12, '八大关', '万国建筑博览会，最美红瓦绿树。', 0, '全天开放', '青岛市市南区', '建筑,摄影,免费,文艺'),
(12, '青岛啤酒博物馆', '百年青岛啤酒厂改建，可品尝原浆。', 60, '08:30-17:30', '青岛市市北区', '啤酒,工业旅游,体验'),
(12, '崂山', '海上第一名山，道教圣地。', 180, '07:00-18:00', '青岛市崂山区', '山岳,道教,自然,海景');

-- 苏州景点（destination_id = 14）
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(14, '拙政园', '中国四大名园之首，苏州园林代表。', 80, '07:30-17:30', '苏州市姑苏区', '园林,世界遗产,文化'),
(14, '虎丘', '吴中第一名胜，苏东坡曾赞叹。', 80, '07:30-18:00', '苏州市虎丘区', '园林,历史,文化'),
(14, '平江路', '老苏州缩影，最有韵味的古街。', 0, '全天开放', '苏州市姑苏区', '古街,文艺,免费,美食'),
(14, '周庄古镇', '中国第一水乡，江南古镇代表。', 100, '08:00-21:00', '苏州市昆山市', '水乡,古镇,文化,摄影'),
(14, '同里古镇', '世外桃源，退思园精美绝伦。', 100, '08:00-17:15', '苏州市吴江区', '水乡,古镇,园林');

-- 拉萨景点（destination_id = 15）
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(15, '布达拉宫', '藏传佛教圣地，西藏的象征。', 200, '09:00-16:00', '拉萨市城关区', '宫殿,宗教,世界遗产,地标'),
(15, '大昭寺', '藏传佛教最神圣的寺庙。', 85, '09:00-18:30', '拉萨市城关区', '寺庙,宗教,朝圣'),
(15, '八廓街', '围绕大昭寺的转经道，藏族文化街区。', 0, '全天开放', '拉萨市城关区', '文化,购物,免费,宗教'),
(15, '纳木错', '西藏三大圣湖之一，天湖美景。', 120, '全天开放', '拉萨市当雄县', '湖泊,高原,圣湖,自然');

-- 重庆景点（destination_id = 16）
INSERT INTO attraction (destination_id, name, description, ticket_price, open_time, address, tags) VALUES
(16, '洪崖洞', '巴渝传统吊脚楼，千与千寻同款。', 0, '全天开放', '重庆市渝中区', '地标,夜景,免费,网红'),
(16, '解放碑', '重庆城市地标，商业中心。', 0, '全天开放', '重庆市渝中区', '地标,购物,免费'),
(16, '磁器口古镇', '千年古镇，重庆特色小吃聚集地。', 0, '全天开放', '重庆市沙坪坝区', '古镇,美食,免费'),
(16, '长江索道', '空中公交，俯瞰两江风光。', 30, '07:30-22:00', '重庆市渝中区', '索道,城市,体验'),
(16, '武隆天生三桥', '世界自然遗产，变形金刚取景地。', 135, '08:30-16:00', '重庆市武隆区', '地质,电影取景,自然,世界遗产');

-- =====================================================
-- 新目的地酒店
-- =====================================================

INSERT INTO hotel (destination_id, name, star_level, description, address, price_min, price_max) VALUES
(9, '张家界京武铂尔曼酒店', 5, '张家界顶级酒店，近景区入口。', '张家界市武陵源区', 800, 2500),
(9, '张家界禾田居度假酒店', 4, '山水环绕的度假酒店。', '张家界市武陵源区', 400, 1000),
(10, '黄山悦榕庄', 5, '黄山脚下的顶级度假村。', '黄山市黄山区', 2500, 6000),
(10, '宏村悦榕庄', 5, '古村旁的奢华体验。', '黄山市黟县', 2000, 5000),
(11, '九寨沟希尔顿度假酒店', 5, '九寨沟最佳酒店选择。', '阿坝州九寨沟县', 1500, 3500),
(12, '青岛香格里拉大酒店', 5, '海滨五星级酒店。', '青岛市市南区', 800, 2500),
(12, '青岛栈桥王子酒店', 4, '栈桥旁的海景酒店。', '青岛市市南区', 400, 1000),
(14, '苏州柏悦酒店', 5, '金鸡湖畔顶级酒店。', '苏州市工业园区', 1500, 4000),
(14, '周庄水月周庄铂尔曼', 5, '周庄古镇旁奢华水乡酒店。', '苏州市昆山市', 1200, 3000),
(15, '拉萨瑞吉酒店', 5, '拉萨顶级酒店，俯瞰布达拉宫。', '拉萨市城关区', 2000, 5000),
(15, '拉萨八廓街民宿', 3, '八廓街旁的藏式民宿。', '拉萨市城关区', 200, 500),
(16, '重庆来福士洲际酒店', 5, '朝天门来福士顶层酒店。', '重庆市渝中区', 1000, 3000),
(16, '重庆洪崖洞旁民宿', 3, '洪崖洞旁的夜景民宿。', '重庆市渝中区', 200, 600);

-- =====================================================
-- 更多轮播图
-- =====================================================

INSERT INTO banner (title, image_url, link_url, sort_order, status) VALUES
('成都美食文化之旅', '/uploads/banner/chengdu.jpg', '/product/4', 4, 1),
('北京历史文化游', '/uploads/banner/beijing.jpg', '/product/5', 5, 1),
('西安兵马俑之旅', '/uploads/banner/xian.jpg', '/product/6', 6, 1),
('桂林山水甲天下', '/uploads/banner/guilin.jpg', '/destinations/7', 7, 1),
('厦门鼓浪屿文艺游', '/uploads/banner/xiamen.jpg', '/destinations/8', 8, 1),
('张家界阿凡达仙境', '/uploads/banner/zhangjiajie.jpg', '/destinations/9', 9, 1),
('黄山日出云海', '/uploads/banner/huangshan.jpg', '/destinations/10', 10, 1),
('九寨沟童话世界', '/uploads/banner/jiuzhaigou.jpg', '/destinations/11', 11, 1);

-- =====================================================
-- 服务商资质数据
-- =====================================================

INSERT INTO provider_qualification (user_id, company_name, license_no, contact_person, contact_phone, audit_status) VALUES
(5, '阳光国际旅行社有限公司', 'L-BJ-CJ00001', '张经理', '13900139001', 1),
(6, '环球之旅旅游服务有限公司', 'L-BJ-CJ00002', '李经理', '13900139002', 1),
(15, '海南岛旅行社有限公司', 'L-HN-CJ00001', '王经理', '13900139003', 1),
(16, '云南印象国际旅行社', 'L-YN-CJ00001', '陈经理', '13900139004', 1),
(17, '江南水乡旅游服务公司', 'L-JS-CJ00001', '林经理', '13900139005', 1);

SELECT '轮播图、服务商资质和新目的地数据插入完成！' AS message;





