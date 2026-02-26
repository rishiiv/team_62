#!/usr/bin/env python3
"""
seed.py (UML-compatible)

Populates the UML tables shown in your diagram:

Core:
- "Customer"
- "Employee"
- "Item"
- "Order"
- inventory_quantity

Junctions:
- "Order_Item"
- "Item_Inventory"

Keeps your requirements:
- 65 weeks of orders
- ~ $1.25M total sales (approx, scaled)
- 4 peak days
- 24 menu items

Notes:
- Quantities per order are stored in "Order".item_quantity (JSONB) as {item_id: qty}
- "Order_Item" stores the (order_id, item_id) relationship (1 row per distinct item per order)
- Customer points = floor($ spent), and purchase_history = array of order_ids
"""

from __future__ import annotations

import argparse
import datetime as dt
import math
import os
import random
import uuid
from dataclasses import dataclass
from typing import Dict, Iterable, List, Tuple

# -------------------------
# Menu Items (24 provided)
# -------------------------
MENU_ITEMS = [
    {"menu_item_id": 1, "name": "Classic Milk Tea", "category": "Milk Tea", "base_price": 4.75, "is_active": True},
    {"menu_item_id": 2, "name": "Taro Milk Tea", "category": "Milk Tea", "base_price": 5.25, "is_active": True},
    {"menu_item_id": 3, "name": "Thai Tea", "category": "Milk Tea", "base_price": 5.00, "is_active": True},
    {"menu_item_id": 4, "name": "Jasmine Green Tea", "category": "Brewed Tea", "base_price": 3.75, "is_active": True},
    {"menu_item_id": 5, "name": "Oolong Tea", "category": "Brewed Tea", "base_price": 3.75, "is_active": True},
    {"menu_item_id": 6, "name": "Wintermelon Tea", "category": "Fruit Tea", "base_price": 4.50, "is_active": True},
    {"menu_item_id": 7, "name": "Mango Green Tea", "category": "Fruit Tea", "base_price": 5.25, "is_active": True},
    {"menu_item_id": 8, "name": "Strawberry Tea", "category": "Fruit Tea", "base_price": 5.25, "is_active": True},
    {"menu_item_id": 9, "name": "Passion Fruit Tea", "category": "Fruit Tea", "base_price": 5.00, "is_active": True},
    {"menu_item_id": 10, "name": "Brown Sugar Boba Milk", "category": "Specialty", "base_price": 5.75, "is_active": True},
    {"menu_item_id": 11, "name": "Matcha Latte", "category": "Specialty", "base_price": 5.75, "is_active": True},
    {"menu_item_id": 12, "name": "Honey Lemon Tea", "category": "Fruit Tea", "base_price": 4.75, "is_active": True},
    {"menu_item_id": 13, "name": "Lychee Tea", "category": "Fruit Tea", "base_price": 5.00, "is_active": True},
    {"menu_item_id": 14, "name": "Peach Oolong Tea", "category": "Fruit Tea", "base_price": 5.25, "is_active": True},
    {"menu_item_id": 15, "name": "Brown Sugar Milk Tea", "category": "Milk Tea", "base_price": 5.50, "is_active": True},
    {"menu_item_id": 16, "name": "Mango Milk Tea", "category": "Milk Tea", "base_price": 5.50, "is_active": True},
    {"menu_item_id": 17, "name": "Strawberry Milk Tea", "category": "Milk Tea", "base_price": 5.50, "is_active": True},
    {"menu_item_id": 18, "name": "Honeydew Milk Tea", "category": "Milk Tea", "base_price": 5.50, "is_active": True},
    {"menu_item_id": 19, "name": "Wintermelon Milk Tea", "category": "Milk Tea", "base_price": 5.50, "is_active": True},
    {"menu_item_id": 20, "name": "Grape Chia", "category": "Milk Tea", "base_price": 5.50, "is_active": True},
    {"menu_item_id": 21, "name": "Passion Fruit", "category": "Milk Tea", "base_price": 5.50, "is_active": True},
    {"menu_item_id": 22, "name": "Oolong Milk Tea", "category": "Milk Tea", "base_price": 5.50, "is_active": True},
    {"menu_item_id": 23, "name": "Honey Lemon Milk Tea", "category": "Milk Tea", "base_price": 5.50, "is_active": True},
    {"menu_item_id": 24, "name": "Peach Milk Tea", "category": "Milk Tea", "base_price": 5.50, "is_active": True},
]


# -------------------------
# Helpers
# -------------------------
def daterange(start: dt.date, end: dt.date) -> Iterable[dt.date]:
    cur = start
    while cur <= end:
        yield cur
        cur += dt.timedelta(days=1)


def clamp(x: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, x))


def weighted_choice(rng: random.Random, items: List[Tuple[int, float]]) -> int:
    """items: [(value, weight), ...]"""
    total = sum(w for _, w in items)
    pick = rng.random() * total
    acc = 0.0
    for val, w in items:
        acc += w
        if pick <= acc:
            return val
    return items[-1][0]


def make_uuid(rng: random.Random) -> uuid.UUID:
    """Deterministic-ish UUIDs from rng (for repeatable seeds)."""
    # 16 random bytes from RNG
    b = bytes(rng.getrandbits(8) for _ in range(16))
    return uuid.UUID(bytes=b, version=4)


# -------------------------
# Config
# -------------------------
@dataclass
class GenConfig:
    start: dt.date
    weeks: int = 65
    seed: int = 42

    target_sales: float = 1_250_000.0
    avg_ticket: float = 10.25

    peak_days: int = 4
    peak_multiplier: float = 4.5  # how much busier peak days are

    open_hour: int = 11
    close_hour: int = 21  # last order start is < close_hour

    customer_n: int = 2000
    employee_n: int = 20

    # seasonality knobs
    dow_mult: Tuple[float, ...] = (0.90, 0.95, 1.00, 1.05, 1.15, 1.30, 1.20)  # Mon..Sun
    month_mult: Tuple[float, ...] = (1.00, 0.98, 1.00, 1.02, 1.04, 1.06, 1.05, 1.05, 1.03, 1.05, 1.15, 1.18)
    daily_noise_sigma: float = 0.12


# -------------------------
# Data generation
# -------------------------
def build_menu_indexes():
    menu_by_id = {m["menu_item_id"]: m for m in MENU_ITEMS}
    base_price = {m["menu_item_id"]: float(m["base_price"]) for m in MENU_ITEMS}

    # popularity weights
    weights: Dict[int, float] = {mid: 1.0 for mid in menu_by_id}
    for m in MENU_ITEMS:
        cat = str(m["category"]).lower()
        mid = int(m["menu_item_id"])
        if "milk" in cat:
            weights[mid] *= 1.25
        if "fruit" in cat:
            weights[mid] *= 1.10
        if "brew" in cat or "tea" in cat:
            weights[mid] *= 0.95
    return menu_by_id, base_price, weights


def generate_customers(cfg: GenConfig, rng: random.Random) -> List[Tuple[uuid.UUID, str, str, str, int, List[uuid.UUID]]]:
    customers = []
    for i in range(1, cfg.customer_n + 1):
        cid = make_uuid(rng)
        name = f"Customer {i}"
        phone = f"({100 + (i % 900):03d}) {100 + ((i * 7) % 900):03d}-{(i * 97) % 10000:04d}"
        email = f"customer{i}@example.com"
        customers.append((cid, name, phone, email, 0, []))
    return customers


def generate_employees(cfg: GenConfig, rng: random.Random) -> List[Tuple[uuid.UUID, str, dt.date, dict]]:
    roles = ["Barista", "Shift Lead", "Manager"]
    employees = []
    base = dt.date(2022, 1, 1)
    for i in range(1, cfg.employee_n + 1):
        eid = make_uuid(rng)
        name = f"Employee {i}"
        start_date = base + dt.timedelta(days=(i * 19) % 700)
        work_history = {"role": roles[(i - 1) % len(roles)], "notes": "Seeded employee record"}
        employees.append((eid, name, start_date, work_history))
    return employees


def generate_items_and_inventory(
    cfg: GenConfig, rng: random.Random
) -> Tuple[
    List[Tuple[uuid.UUID, str, str, float, bool, str, int, float, List[str]]],
    List[Tuple[uuid.UUID, int, dt.date, dt.date]],
    List[Tuple[uuid.UUID, uuid.UUID, uuid.UUID]],
    Dict[int, uuid.UUID],
    Dict[uuid.UUID, float],
]:
    """
    Returns:
      items rows,
      inventory_quantity rows,
      item_inventory rows,
      map menu_item_id -> item_id,
      map item_id -> base_price
    """
    milk_opts = ["Whole", "Oat", "Almond", "None"]
    topping_opts = ["Boba", "Lychee Jelly", "Popping Boba", "Chia", "Aloe"]

    items = []
    inv_rows = []
    junction = []
    menu_to_item: Dict[int, uuid.UUID] = {}
    item_price: Dict[uuid.UUID, float] = {}

    today = cfg.start

    for m in MENU_ITEMS:
        menu_id = int(m["menu_item_id"])
        item_id = make_uuid(rng)
        inv_id = make_uuid(rng)

        name = str(m["name"])
        category = str(m["category"])
        price = float(m["base_price"])
        is_active = bool(m["is_active"])

        # defaults similar to your older seed.sql approach
        milk = milk_opts[menu_id % len(milk_opts)]
        ice = (menu_id * 7) % 3  # 0/1/2
        sugar = (50 + ((menu_id * 13) % 51)) / 100.0  # 0.50..1.00
        toppings = [topping_opts[menu_id % len(topping_opts)]]

        items.append((item_id, name, category, price, is_active, milk, ice, sugar, toppings))
        menu_to_item[menu_id] = item_id
        item_price[item_id] = price

        # inventory_quantity: start with a decent baseline, will subtract sold later
        start_qty = 8000 + ((menu_id * 137) % 2500)
        inv_rows.append((inv_id, int(start_qty), today, today))

        # Item_Inventory junction
        junction.append((make_uuid(rng), inv_id, item_id))

    return items, inv_rows, junction, menu_to_item, item_price


def pick_peak_days(cfg: GenConfig, rng: random.Random) -> List[dt.date]:
    end = cfg.start + dt.timedelta(days=cfg.weeks * 7 - 1)
    all_days = list(daterange(cfg.start, end))

    # Evenly spread peaks across the window, with slight jitter
    peaks: List[dt.date] = []
    if cfg.peak_days <= 0:
        return peaks

    step = len(all_days) / (cfg.peak_days + 1)
    for k in range(1, cfg.peak_days + 1):
        center = int(round(k * step))
        jitter = rng.randint(-5, 5)
        idx = clamp(center + jitter, 0, len(all_days) - 1)
        peaks.append(all_days[int(idx)])

    # ensure unique
    peaks = sorted(set(peaks))
    while len(peaks) < cfg.peak_days:
        peaks.append(rng.choice(all_days))
        peaks = sorted(set(peaks))
    return peaks[: cfg.peak_days]


def generate_orders(
    cfg: GenConfig,
    rng: random.Random,
    customers: List[uuid.UUID],
    employees: List[uuid.UUID],
    menu_to_item: Dict[int, uuid.UUID],
    item_price: Dict[uuid.UUID, float],
) -> Tuple[
    List[Tuple[uuid.UUID, dict, uuid.UUID, uuid.UUID, dt.datetime]],
    List[Tuple[uuid.UUID, uuid.UUID, uuid.UUID]],
    Dict[uuid.UUID, float],
    Dict[uuid.UUID, List[uuid.UUID]],
]:
    """
    Returns:
      order_rows: (order_id, item_quantity_json, employee_id, customer_id, timestamp)
      order_item_rows: (id, order_id, item_id)  [1 row per distinct item in the order]
      order_totals: order_id -> total spend
      customer_orders: customer_id -> list of order_ids
    """
    end = cfg.start + dt.timedelta(days=cfg.weeks * 7 - 1)
    n_days = (end - cfg.start).days + 1

    # approximate number of orders based on avg ticket
    approx_orders_total = int(max(1, round(cfg.target_sales / cfg.avg_ticket)))
    avg_orders_per_day = approx_orders_total / n_days

    peaks = set(pick_peak_days(cfg, rng))

    # build popularity list on menu ids
    _, _, pop_weights = build_menu_indexes()
    menu_ids = sorted(menu_to_item.keys())
    choice_list = [(mid, pop_weights[mid]) for mid in menu_ids]

    # day weights with seasonality + noise + peak multiplier
    raw: List[Tuple[dt.date, float]] = []
    for d in daterange(cfg.start, end):
        mult = cfg.dow_mult[d.weekday()] * cfg.month_mult[d.month - 1]
        noise = math.exp(rng.gauss(0, cfg.daily_noise_sigma))
        if d in peaks:
            mult *= cfg.peak_multiplier
        raw.append((d, mult * noise))

    # normalize
    norm = sum(v for _, v in raw) or 1.0

    daily_orders: Dict[dt.date, int] = {}
    for d, v in raw:
        expected = avg_orders_per_day * (v / (norm / n_days))
        lam = max(0.1, expected)
        k = int(max(0, round(rng.gauss(lam, math.sqrt(lam)))))  # Poisson-ish
        daily_orders[d] = k

    order_rows: List[Tuple[uuid.UUID, dict, uuid.UUID, uuid.UUID, dt.datetime]] = []
    order_item_rows: List[Tuple[uuid.UUID, uuid.UUID, uuid.UUID]] = []
    order_totals: Dict[uuid.UUID, float] = {}
    customer_orders: Dict[uuid.UUID, List[uuid.UUID]] = {c: [] for c in customers}

    for d, count in daily_orders.items():
        for _ in range(count):
            order_id = make_uuid(rng)

            hour = rng.randint(cfg.open_hour, cfg.close_hour - 1)
            minute = rng.choice([0, 5, 10, 12, 15, 18, 20, 25, 30, 35, 40, 45, 50, 55])
            second = rng.choice([0, 0, 0, 30])
            order_dt = dt.datetime(d.year, d.month, d.day, hour, minute, second)

            customer_id = rng.choice(customers)
            employee_id = rng.choice(employees)

            # number of distinct line items
            n_items = weighted_choice(rng, [(1, 0.55), (2, 0.33), (3, 0.12)])
            item_qty: Dict[str, int] = {}  # JSON wants string keys in many clients
            distinct_item_ids: List[uuid.UUID] = []
            total = 0.0

            for _i in range(n_items):
                mid = weighted_choice(rng, choice_list)
                item_id = menu_to_item[mid]
                qty = weighted_choice(rng, [(1, 0.78), (2, 0.20), (3, 0.02)])

                # small promo variance
                price = float(item_price[item_id])
                price = round(price + rng.choice([0.00, 0.00, 0.25, 0.50, -0.25]), 2)
                price = max(2.50, price)

                # accumulate qty per item_id
                key = str(item_id)
                item_qty[key] = item_qty.get(key, 0) + qty
                if item_id not in distinct_item_ids:
                    distinct_item_ids.append(item_id)

                total += price * qty

            total = round(total, 2)
            order_totals[order_id] = total
            customer_orders[customer_id].append(order_id)

            order_rows.append((order_id, item_qty, employee_id, customer_id, order_dt))

            # one row per distinct item in the order (qty is in Order.item_quantity JSONB)
            for item_id in distinct_item_ids:
                order_item_rows.append((make_uuid(rng), order_id, item_id))

    # scale totals to target (within +/-25% so it stays realistic)
    current_total = sum(order_totals.values()) or 1.0
    factor = cfg.target_sales / current_total
    factor = clamp(factor, 0.75, 1.25)

    if abs(1.0 - factor) > 0.02:
        # Instead of rewriting all item prices, we adjust *points/total* by scaling when computing points.
        # But your schema doesn't store order totals anyway.
        # We'll scale "order_totals" so points & reporting match target.
        for oid in list(order_totals.keys()):
            order_totals[oid] = round(order_totals[oid] * factor, 2)

    return order_rows, order_item_rows, order_totals, customer_orders


def compute_customer_points_and_history(
    customers: List[uuid.UUID],
    customer_orders: Dict[uuid.UUID, List[uuid.UUID]],
    order_totals: Dict[uuid.UUID, float],
) -> List[Tuple[uuid.UUID, int, List[uuid.UUID]]]:
    updates = []
    for cid in customers:
        oids = customer_orders.get(cid, [])
        spend = sum(order_totals.get(oid, 0.0) for oid in oids)
        pts = int(math.floor(spend))
        updates.append((cid, pts, oids))
    return updates


def compute_inventory_after_sales(
    inv_map_item_to_inventory: Dict[uuid.UUID, uuid.UUID],
    order_rows: List[Tuple[uuid.UUID, dict, uuid.UUID, uuid.UUID, dt.datetime]],
    initial_inventory: Dict[uuid.UUID, int],
) -> Dict[uuid.UUID, int]:
    """
    Very simple model: subtract "drink count" from each item's inventory bucket.
    (Since your UML inventory is per-item, not ingredient-level.)
    """
    remaining = dict(initial_inventory)
    for (_oid, item_qty, _eid, _cid, _ts) in order_rows:
        for item_id_str, qty in item_qty.items():
            item_id = uuid.UUID(item_id_str)
            inv_id = inv_map_item_to_inventory[item_id]
            remaining[inv_id] = max(0, remaining.get(inv_id, 0) - int(qty))
    return remaining


# -------------------------
# DB execution
# -------------------------
def execute_seed(cfg: GenConfig, dsn: str, truncate_first: bool = False) -> None:
    try:
        import psycopg2
        from psycopg2.extras import execute_values, Json, register_uuid
    except ImportError as e:
        raise SystemExit("psycopg2 is required. Install with: pip install psycopg2-binary") from e

    rng = random.Random(cfg.seed)

    # Generate entities
    customers_full = generate_customers(cfg, rng)
    employees_full = generate_employees(cfg, rng)

    items_full, inv_full, item_inv_full, menu_to_item, item_price = generate_items_and_inventory(cfg, rng)

    customer_ids = [c[0] for c in customers_full]
    employee_ids = [e[0] for e in employees_full]

    # orders + order_items
    order_rows, order_item_rows, order_totals, customer_orders = generate_orders(
        cfg, rng, customer_ids, employee_ids, menu_to_item, item_price
    )

    # update customer points + purchase history
    cust_updates = compute_customer_points_and_history(customer_ids, customer_orders, order_totals)

    # inventory recompute (per-item)
    # Build item->inventory from Item_Inventory rows
    inv_map_item_to_inventory = {item_id: inv_id for (_jid, inv_id, item_id) in item_inv_full}
    initial_inventory = {inv_id: qty for (inv_id, qty, _lr, _lq) in inv_full}
    remaining_inventory = compute_inventory_after_sales(inv_map_item_to_inventory, order_rows, initial_inventory)

    # Prepare DB writes
    with psycopg2.connect(dsn) as conn:
        register_uuid(conn_or_curs=conn)
        conn.autocommit = False
        with conn.cursor() as cur:
            if truncate_first:
                # Order matters; CASCADE makes it robust.
                cur.execute(
                    """
                    TRUNCATE TABLE
                      "Order_Item",
                      "Item_Inventory",
                      "Order",
                      inventory_quantity,
                      "Item",
                      "Employee",
                      "Customer"
                    CASCADE;
                    """
                )

            # Customers
            execute_values(
                cur,
                """
                INSERT INTO "Customer"(customer_id, name, phone_number, email, points, purchase_history)
                VALUES %s
                """,
                [(cid, name, phone, email, pts, oids) for (cid, name, phone, email, pts, oids) in customers_full],
                page_size=1000,
            )

            # Employees
            execute_values(
                cur,
                """
                INSERT INTO "Employee"(employee_id, name, start_date, work_history)
                VALUES %s
                """,
                [(eid, name, sdate, Json(wh)) for (eid, name, sdate, wh) in employees_full],
                page_size=1000,
            )

            # Items
            execute_values(
                cur,
                """
                INSERT INTO "Item"(item_id, name, category, price, is_active, milk, ice, sugar, toppings)
                VALUES %s
                """,
                [
                    (iid, name, cat, float(price), bool(active), milk, int(ice), float(sugar), toppings)
                    for (iid, name, cat, price, active, milk, ice, sugar, toppings) in items_full
                ],
                page_size=500,
            )

            # inventory_quantity
            execute_values(
                cur,
                """
                INSERT INTO inventory_quantity(inventory_id, quantity, last_restocked, last_quantity)
                VALUES %s
                """,
                [(inv_id, int(qty), lr, lq) for (inv_id, qty, lr, lq) in inv_full],
                page_size=500,
            )

            # Item_Inventory
            execute_values(
                cur,
                """
                INSERT INTO "Item_Inventory"(id, inventory_id, item_id)
                VALUES %s
                """,
                item_inv_full,
                page_size=500,
            )

            # Orders
            execute_values(
                cur,
                """
                INSERT INTO "Order"(order_id, item_quantity, employee_id, customer_id, date)
                VALUES %s
                """,
                [(oid, Json(item_qty), eid, cid, ts) for (oid, item_qty, eid, cid, ts) in order_rows],
                page_size=2000,
            )

            # Order_Item
            execute_values(
                cur,
                """
                INSERT INTO "Order_Item"(id, order_id, item_id)
                VALUES %s
                """,
                order_item_rows,
                page_size=5000,
            )

            # Update inventory quantities after sales
            execute_values(
                cur,
                """
                UPDATE inventory_quantity AS iq
                SET quantity = v.quantity
                FROM (VALUES %s) AS v(inventory_id, quantity)
                WHERE iq.inventory_id = v.inventory_id
                """,
                [(inv_id, qty) for inv_id, qty in remaining_inventory.items()],
                page_size=1000,
            )

            # Update customer points + purchase_history
            execute_values(
                cur,
                """
                UPDATE "Customer" AS c
                SET points = v.points,
                    purchase_history = v.purchase_history
                FROM (VALUES %s) AS v(customer_id, points, purchase_history)
                WHERE c.customer_id = v.customer_id
                """,
                [(cid, pts, oids) for (cid, pts, oids) in cust_updates],
                page_size=1000,
            )

        conn.commit()

    total_sales = sum(order_totals.values())
    print(
        "Seed complete.\n"
        f"  Customers: {len(customers_full)}\n"
        f"  Employees: {len(employees_full)}\n"
        f"  Items:     {len(items_full)}\n"
        f"  Orders:    {len(order_rows)}\n"
        f"  Peak days: {cfg.peak_days}\n"
        f"  Sales ≈    ${total_sales:,.2f}\n"
    )


# -------------------------
# CLI
# -------------------------
def parse_args():
    ap = argparse.ArgumentParser()
    ap.add_argument("--start", default="2024-01-01", help="Start date (YYYY-MM-DD)")
    ap.add_argument("--weeks", type=int, default=65, help="Number of weeks to generate")
    ap.add_argument("--seed", type=int, default=42, help="Random seed")
    ap.add_argument("--target-sales", type=float, default=1_250_000.0, help="Approx total sales for whole range")
    ap.add_argument("--avg-ticket", type=float, default=10.25, help="Average ticket size (used to estimate order counts)")
    ap.add_argument("--peak-days", type=int, default=4, help="Number of peak days")
    ap.add_argument("--execute", action="store_true", help="Execute inserts into Postgres using --dsn")
    ap.add_argument("--dsn", default=os.environ.get("PG_DSN", ""), help="psycopg2 DSN string (or set PG_DSN env var)")
    ap.add_argument("--truncate-first", action="store_true", help="TRUNCATE tables before inserting (destructive)")
    return ap.parse_args()


def main():
    args = parse_args()
    start = dt.date.fromisoformat(args.start)
    cfg = GenConfig(
        start=start,
        weeks=args.weeks,
        seed=args.seed,
        target_sales=args.target_sales,
        avg_ticket=args.avg_ticket,
        peak_days=args.peak_days,
    )

    if not args.execute:
        # Dry run stats
        rng = random.Random(cfg.seed)
        customers = generate_customers(cfg, rng)
        employees = generate_employees(cfg, rng)
        items, inv, item_inv, menu_to_item, item_price = generate_items_and_inventory(cfg, rng)
        order_rows, order_item_rows, order_totals, _customer_orders = generate_orders(
            cfg,
            rng,
            [c[0] for c in customers],
            [e[0] for e in employees],
            menu_to_item,
            item_price,
        )
        total_sales = sum(order_totals.values())
        end = cfg.start + dt.timedelta(days=cfg.weeks * 7 - 1)
        print(
            "Dry run:\n"
            f"  Range: {cfg.start} .. {end}  ({cfg.weeks} weeks)\n"
            f"  Customers: {len(customers)}\n"
            f"  Employees: {len(employees)}\n"
            f"  Items: {len(items)}\n"
            f"  Orders: {len(order_rows)}\n"
            f"  Order_Item rows: {len(order_item_rows)}\n"
            f"  Sales ≈ ${total_sales:,.2f}\n"
        )
        print("\nRun with --execute --dsn '...' to insert into Postgres.")
        return

    if not args.dsn:
        raise SystemExit("--execute requires --dsn (or PG_DSN env var)")

    execute_seed(cfg, args.dsn, truncate_first=args.truncate_first)


if __name__ == "__main__":
    main()