CREATE TABLE IF NOT EXISTS menu_items (
  menu_item_id INT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(50) NOT NULL,
  base_price DECIMAL(6,2) NOT NULL,
  is_active BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_items (
  inventory_item_id INT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  par_level INT NOT NULL,
  reorder_point INT NOT NULL,
  is_active BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS menu_item_ingredients (
  menu_item_id INT NOT NULL,
  inventory_item_id INT NOT NULL,
  qty_per_item INT NOT NULL,
  PRIMARY KEY (menu_item_id, inventory_item_id)
);

CREATE TABLE IF NOT EXISTS sales_orders (
  order_id BIGINT PRIMARY KEY,
  order_datetime TIMESTAMP NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  payment_method VARCHAR(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS sales_order_items (
  order_item_id BIGINT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  menu_item_id INT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(6,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_purchases (
  purchase_id BIGINT PRIMARY KEY,
  inventory_item_id INT NOT NULL,
  purchase_date DATE NOT NULL,
  quantity INT NOT NULL,
  unit_cost DECIMAL(10,4) NOT NULL,
  line_cost DECIMAL(12,4) NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_usage (
  usage_id BIGINT PRIMARY KEY,
  inventory_item_id INT NOT NULL,
  usage_date DATE NOT NULL,
  quantity INT NOT NULL
);
