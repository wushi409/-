-- init 相关 SQL，先保证语句正确，后面再做性能优化。
-- 创建数据库
CREATE DATABASE IF NOT EXISTS travel_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travel_db;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(50),
  avatar VARCHAR(255),
  phone VARCHAR(20),
  email VARCHAR(100),
  role TINYINT NOT NULL COMMENT '0-管理员 1-游客 2-服务商',
  status TINYINT DEFAULT 1 COMMENT '0-禁用 1-正常',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 服务商资质表
CREATE TABLE IF NOT EXISTS provider_qualification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  company_name VARCHAR(100),
  license_no VARCHAR(100),
  license_image VARCHAR(255),
  contact_person VARCHAR(50),
  contact_phone VARCHAR(20),
  audit_status TINYINT DEFAULT 0 COMMENT '0-待审核 1-通过 2-拒绝',
  audit_remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 目的地表
CREATE TABLE IF NOT EXISTS destination (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  province VARCHAR(50),
  city VARCHAR(50),
  description TEXT,
  cover_image VARCHAR(255),
  images TEXT COMMENT '多图JSON数组',
  longitude DECIMAL(10,6),
  latitude DECIMAL(10,6),
  hot_score INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 景点表
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
  tags VARCHAR(255) COMMENT '标签,逗号分隔',
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 酒店表
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
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 交通资源表
CREATE TABLE IF NOT EXISTS transport (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type TINYINT COMMENT '0-飞机 1-火车 2-大巴 3-租车',
  departure VARCHAR(100),
  arrival VARCHAR(100),
  price DECIMAL(10,2),
  description TEXT,
  provider_id BIGINT,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 旅游产品表
CREATE TABLE IF NOT EXISTS travel_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  provider_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  cover_image VARCHAR(255),
  images TEXT,
  destination_id BIGINT,
  duration INT COMMENT '行程天数',
  price DECIMAL(10,2),
  original_price DECIMAL(10,2),
  product_type TINYINT COMMENT '0-跟团游 1-自由行 2-当地向导 3-特色路线',
  tags VARCHAR(255),
  include_items TEXT COMMENT '费用包含',
  exclude_items TEXT COMMENT '费用不含',
  stock INT DEFAULT 999,
  sales INT DEFAULT 0,
  status TINYINT DEFAULT 1 COMMENT '0-下架 1-上架',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 行程日计划表
CREATE TABLE IF NOT EXISTS travel_day_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  day_number INT NOT NULL,
  title VARCHAR(200),
  description TEXT,
  attraction_ids VARCHAR(255),
  hotel_id BIGINT,
  transport_id BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 定制需求表
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
  status TINYINT DEFAULT 0 COMMENT '0-待处理 1-方案已出 2-已确认 3-已取消',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 定制方案表
CREATE TABLE IF NOT EXISTS custom_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  provider_id BIGINT NOT NULL,
  title VARCHAR(200),
  description TEXT,
  total_price DECIMAL(10,2),
  day_plans TEXT COMMENT '每日行程JSON',
  status TINYINT DEFAULT 0 COMMENT '0-待确认 1-已接受 2-已拒绝',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. 订单表
CREATE TABLE IF NOT EXISTS travel_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) UNIQUE NOT NULL,
  user_id BIGINT NOT NULL,
  provider_id BIGINT NOT NULL,
  product_id BIGINT,
  custom_plan_id BIGINT,
  order_type TINYINT COMMENT '0-产品订单 1-定制订单',
  total_amount DECIMAL(10,2),
  status TINYINT DEFAULT 0 COMMENT '0-待付款 1-已付款 2-进行中 3-已完成 4-已取消 5-退款中 6-已退款',
  people_count INT,
  travel_date DATE,
  contact_name VARCHAR(50),
  contact_phone VARCHAR(20),
  remark TEXT,
  pay_time DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  msg_type TINYINT DEFAULT 0 COMMENT '0-文本 1-图片',
  is_read TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. 会话表
CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider_id BIGINT NOT NULL,
  last_message TEXT,
  last_time DATETIME,
  unread_count INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. 用户行为记录表
CREATE TABLE IF NOT EXISTS user_behavior (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  behavior_type TINYINT COMMENT '0-浏览 1-收藏 2-购买 3-评分',
  score DECIMAL(3,1),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id),
  INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. 用户收藏表
CREATE TABLE IF NOT EXISTS user_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. 评价表
CREATE TABLE IF NOT EXISTS review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  order_id BIGINT,
  rating TINYINT COMMENT '1-5星',
  content TEXT,
  images TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 17. 轮播图表
CREATE TABLE IF NOT EXISTS banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100),
  image_url VARCHAR(255),
  link_url VARCHAR(255),
  sort_order INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 初始数据 =====

-- 管理员账号 (密码: admin123)
INSERT INTO sys_user (username, password, nickname, role, status) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 0, 1);

-- 测试游客账号 (密码: 123456)
INSERT INTO sys_user (username, password, nickname, phone, role, status) VALUES
('user1', 'e10adc3949ba59abbe56e057f20f883e', '张三', '13800138001', 1, 1),
('user2', 'e10adc3949ba59abbe56e057f20f883e', '李四', '13800138002', 1, 1),
('user3', 'e10adc3949ba59abbe56e057f20f883e', '王五', '13800138003', 1, 1);

-- 测试服务商账号 (密码: 123456)
INSERT INTO sys_user (username, password, nickname, phone, role, status) VALUES
('provider1', 'e10adc3949ba59abbe56e057f20f883e', '阳光旅行社', '13900139001', 2, 1),
('provider2', 'e10adc3949ba59abbe56e057f20f883e', '环球旅游', '13900139002', 2, 1);

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
