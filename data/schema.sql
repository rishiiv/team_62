BEGIN;

-- =========================================================
-- Optional cleanup: remove the old 7-table analytics schema
-- (comment this block out if you want to keep those tables)
-- =========================================================
DROP TABLE IF EXISTS
  sales_order_items,
  sales_orders,
  inventory_purchases,
  inventory_usage,
  menu_item_ingredients,
  inventory_items,
  menu_items
CASCADE;

-- =========================================================
-- UML SCHEMA (matches adapted seed.py)
-- =========================================================

-- -------------------------
-- Customer
-- -------------------------
CREATE TABLE IF NOT EXISTS "Customer" (
  customer_id       UUID PRIMARY KEY,
  name              TEXT NOT NULL,
  phone_number      TEXT,
  email             TEXT,
  points            INTEGER NOT NULL DEFAULT 0 CHECK (points >= 0),
  purchase_history  UUID[] NOT NULL DEFAULT '{}'::UUID[]
);

-- Optional uniqueness (safe if you want it)
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_email ON "Customer"(email);

-- -------------------------
-- Employee
-- -------------------------
CREATE TABLE IF NOT EXISTS "Employee" (
  employee_id   UUID PRIMARY KEY,
  name          TEXT NOT NULL,
  start_date    DATE NOT NULL,
  work_history  JSONB NOT NULL DEFAULT '{}'::JSONB
);

-- -------------------------
-- Item (menu items)
-- -------------------------
CREATE TABLE IF NOT EXISTS "Item" (
  item_id     UUID PRIMARY KEY,
  name        TEXT NOT NULL,
  category    TEXT NOT NULL,
  price       NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  is_active   BOOLEAN NOT NULL DEFAULT TRUE,

  -- customization fields (as in your UML/seed)
  milk        TEXT NOT NULL,
  ice         SMALLINT NOT NULL CHECK (ice IN (0,1,2)),
  sugar       REAL NOT NULL CHECK (sugar >= 0.0 AND sugar <= 1.0),
  toppings    TEXT[] NOT NULL DEFAULT '{}'::TEXT[]
);

-- Helpful if you want to prevent exact duplicates:
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_item_name_category ON "Item"(name, category);

-- -------------------------
-- inventory_quantity
-- -------------------------
CREATE TABLE IF NOT EXISTS inventory_quantity (
  inventory_id     UUID PRIMARY KEY,
  quantity         INTEGER NOT NULL CHECK (quantity >= 0),
  last_restocked   DATE NOT NULL,
  last_quantity    DATE NOT NULL
);

-- -------------------------
-- Item_Inventory (junction)
-- Each item has an inventory row (1:1-ish),
-- but we keep it as a junction to match UML + seed.
-- -------------------------
CREATE TABLE IF NOT EXISTS "Item_Inventory" (
  id            UUID PRIMARY KEY,
  inventory_id  UUID NOT NULL,
  item_id       UUID NOT NULL,

  CONSTRAINT fk_item_inventory_inventory
    FOREIGN KEY (inventory_id)
    REFERENCES inventory_quantity(inventory_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,

  CONSTRAINT fk_item_inventory_item
    FOREIGN KEY (item_id)
    REFERENCES "Item"(item_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

-- prevent accidental duplicates
CREATE UNIQUE INDEX IF NOT EXISTS uq_item_inventory_pair
  ON "Item_Inventory"(inventory_id, item_id);

-- Also enforce "one inventory per item" if you want strict 1:1:
CREATE UNIQUE INDEX IF NOT EXISTS uq_item_inventory_item
  ON "Item_Inventory"(item_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_item_inventory_inventory
  ON "Item_Inventory"(inventory_id);

-- -------------------------
-- Order (quoted: reserved word)
-- item_quantity is JSONB: { "item_uuid_string": qty_int, ... }
-- -------------------------
CREATE TABLE IF NOT EXISTS "Order" (
  order_id       UUID PRIMARY KEY,
  item_quantity  JSONB NOT NULL DEFAULT '{}'::JSONB,
  employee_id    UUID NOT NULL,
  customer_id    UUID NOT NULL,
  date           TIMESTAMPTZ NOT NULL,

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

CREATE INDEX IF NOT EXISTS idx_order_date ON "Order"(date);
CREATE INDEX IF NOT EXISTS idx_order_customer ON "Order"(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_employee ON "Order"(employee_id);

-- -------------------------
-- Order_Item (junction)
-- One row per distinct item in the order.
-- Quantity is stored in Order.item_quantity JSONB.
-- -------------------------
CREATE TABLE IF NOT EXISTS "Order_Item" (
  id        UUID PRIMARY KEY,
  order_id  UUID NOT NULL,
  item_id   UUID NOT NULL,

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

CREATE INDEX IF NOT EXISTS idx_order_item_order ON "Order_Item"(order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_item ON "Order_Item"(item_id);

-- prevent duplicates: same item listed twice for same order
CREATE UNIQUE INDEX IF NOT EXISTS uq_order_item_pair
  ON "Order_Item"(order_id, item_id);

COMMIT;