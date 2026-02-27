BEGIN;

-- Clean slate (safe)
DROP VIEW IF EXISTS inventory_quantity CASCADE;

DROP TABLE IF EXISTS
  "Order_Item",
  "Item_Inventory",
  "Order",
  "Inventory_Quantity",
  "Item",
  "Employee",
  "Customer"
CASCADE;

-- -------------------------
-- Customer
-- -------------------------
CREATE TABLE "Customer" (
  customer_id       UUID PRIMARY KEY,
  name              TEXT NOT NULL,
  phone_number      TEXT,
  email             TEXT,
  points            INTEGER NOT NULL DEFAULT 0 CHECK (points >= 0),
  purchase_history  UUID[] NOT NULL DEFAULT '{}'::UUID[]
);

-- -------------------------
-- Employee
-- -------------------------
CREATE TABLE "Employee" (
  employee_id   UUID PRIMARY KEY,
  name          TEXT NOT NULL,
  start_date    DATE NOT NULL,
  work_history  JSONB NOT NULL DEFAULT '{}'::JSONB
);

-- -------------------------
-- Item (menu items)
-- -------------------------
CREATE TABLE "Item" (
  item_id     UUID PRIMARY KEY,
  name        TEXT NOT NULL,
  category    TEXT NOT NULL,
  price       NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  is_active   BOOLEAN NOT NULL DEFAULT TRUE,

  -- customization fields
  milk        TEXT NOT NULL,
  ice         SMALLINT NOT NULL CHECK (ice IN (0,1,2)),
  sugar       REAL NOT NULL CHECK (sugar >= 0.0 AND sugar <= 1.0),
  toppings    TEXT[] NOT NULL DEFAULT '{}'::TEXT[]
);

-- -------------------------
-- Inventory_Quantity (matches MainController expectations)
-- NOTE: last_quantity is a DATE in your original schema; keeping it.
-- -------------------------
CREATE TABLE "Inventory_Quantity" (
  inventory_id     UUID PRIMARY KEY,
  quantity         INTEGER NOT NULL CHECK (quantity >= 0),
  last_restocked   DATE NOT NULL,
  last_quantity    DATE NOT NULL
);

-- Compatibility view: allow code/queries that reference inventory_quantity (lowercase)
CREATE VIEW inventory_quantity AS
SELECT inventory_id, quantity, last_restocked, last_quantity
FROM "Inventory_Quantity";

-- -------------------------
-- Item_Inventory (junction)
-- -------------------------
CREATE TABLE "Item_Inventory" (
  id            UUID PRIMARY KEY,
  inventory_id  UUID NOT NULL,
  item_id       UUID NOT NULL,

  CONSTRAINT fk_item_inventory_inventory
    FOREIGN KEY (inventory_id)
    REFERENCES "Inventory_Quantity"(inventory_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,

  CONSTRAINT fk_item_inventory_item
    FOREIGN KEY (item_id)
    REFERENCES "Item"(item_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_item_inventory_pair
  ON "Item_Inventory"(inventory_id, item_id);

CREATE UNIQUE INDEX uq_item_inventory_item
  ON "Item_Inventory"(item_id);

CREATE UNIQUE INDEX uq_item_inventory_inventory
  ON "Item_Inventory"(inventory_id);

-- -------------------------
-- Order (quoted: reserved word)
-- Includes BOTH:
--  - item_quantity JSONB (UML)
--  - total_price (MainController)
-- -------------------------
CREATE TABLE "Order" (
  order_id       UUID PRIMARY KEY,
  item_quantity  JSONB NOT NULL DEFAULT '{}'::JSONB,
  employee_id    UUID NOT NULL,
  customer_id    UUID NOT NULL,
  date           TIMESTAMPTZ NOT NULL,
  total_price    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_price >= 0),

  CONSTRAINT fk_order_employee
    FOREIGN KEY (employee_id)
    REFERENCES "Employee"(employee_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,

  CONSTRAINT fk_order_customer
    FOREIGN KEY (customer_id)
    REFERENCES "Customer"(customer_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
);

CREATE INDEX idx_order_date ON "Order"(date);
CREATE INDEX idx_order_customer ON "Order"(customer_id);
CREATE INDEX idx_order_employee ON "Order"(employee_id);

-- -------------------------
-- Order_Item (junction)
-- Includes BOTH:
--  - (order_id, item_id) row per distinct item (UML)
--  - quantity + unit_price (MainController + reporting)
-- -------------------------
CREATE TABLE "Order_Item" (
  id         UUID PRIMARY KEY,
  order_id   UUID NOT NULL,
  item_id    UUID NOT NULL,
  quantity   INTEGER NOT NULL CHECK (quantity > 0),
  unit_price NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),

  CONSTRAINT fk_order_item_order
    FOREIGN KEY (order_id)
    REFERENCES "Order"(order_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,

  CONSTRAINT fk_order_item_item
    FOREIGN KEY (item_id)
    REFERENCES "Item"(item_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
);

CREATE INDEX idx_order_item_order ON "Order_Item"(order_id);
CREATE INDEX idx_order_item_item ON "Order_Item"(item_id);

CREATE UNIQUE INDEX uq_order_item_pair
  ON "Order_Item"(order_id, item_id);

COMMIT;