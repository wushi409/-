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

CREATE TABLE IF NOT EXISTS banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100),
  image_url VARCHAR(255),
  link_url VARCHAR(255),
  sort_order INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
