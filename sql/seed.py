#!/usr/bin/env python
"""
sql/seed.py

Seeds schema.sql tables:

Core:
- "Customer"
- "Employee"
- "Item"
- "Order"
- "Inventory_Quantity"

Junctions:
- "Order_Item"
- "Item_Inventory"

Defaults for team size 6:
- 65 weeks (ending about today)
- ~ $1.25M sales
- 4 peak days
- 24 menu items

Important:
- "Order" includes item_quantity JSONB AND total_price
- "Order_Item" includes quantity and unit_price (matches MainController + reporting)
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


def daterange(start: dt.date, end: dt.date) -> Iterable[dt.date]:
    cur = start
    while cur <= end:
        yield cur
        cur += dt.timedelta(days=1)


def clamp(x: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, x))


def weighted_choice(rng: random.Random, items: List[Tuple[int, float]]) -> int:
    total = sum(w for _, w in items) or 1.0
    pick = rng.random() * total
    acc = 0.0
    for val, w in items:
        acc += w
        if pick <= acc:
            return val
    return items[-1][0]


def make_uuid(rng: random.Random) -> uuid.UUID:
    b = bytes(rng.getrandbits(8) for _ in range(16))
    return uuid.UUID(bytes=b, version=4)


def default_start_for_weeks(weeks: int) -> dt.date:
    today = dt.date.today()
    return today - dt.timedelta(days=weeks * 7 - 1)


@dataclass
class GenConfig:
    start: dt.date
    weeks: int = 65
    seed: int = 42

    target_sales: float = 1_250_000.0
    avg_ticket: float = 10.25

    peak_days: int = 4
    peak_multiplier: float = 4.5

    open_hour: int = 11
    close_hour: int = 21

    customer_n: int = 2000
    employee_n: int = 20

    dow_mult: Tuple[float, ...] = (0.90, 0.95, 1.00, 1.05, 1.15, 1.30, 1.20)
    month_mult: Tuple[float, ...] = (1.00, 0.98, 1.00, 1.02, 1.04, 1.06, 1.05, 1.05, 1.03, 1.05, 1.15, 1.18)
    daily_noise_sigma: float = 0.12


def build_menu_indexes():
    menu_by_id = {m["menu_item_id"]: m for m in MENU_ITEMS}
    base_price = {m["menu_item_id"]: float(m["base_price"]) for m in MENU_ITEMS}

    weights: Dict[int, float] = {mid: 1.0 for mid in menu_by_id}
    for m in MENU_ITEMS:
        cat = str(m["category"]).lower()
        mid = int(m["menu_item_id"])
        if "milk" in cat:
            weights[mid] *= 1.25
        if "fruit" in cat:
            weights[mid] *= 1.10
        if "brew" in cat:
            weights[mid] *= 0.95
    return menu_by_id, base_price, weights


def generate_customers(cfg: GenConfig, rng: random.Random):
    rows = []
    for i in range(1, cfg.customer_n + 1):
        cid = make_uuid(rng)
        name = f"Customer {i}"
        phone = f"({100 + (i % 900):03d}) {100 + ((i * 7) % 900):03d}-{(i * 97) % 10000:04d}"
        email = f"customer{i}@example.com"
        rows.append((cid, name, phone, email, 0, []))
    return rows


def generate_employees(cfg: GenConfig, rng: random.Random):
    roles = ["Barista", "Shift Lead", "Manager"]
    rows = []
    base = dt.date(2022, 1, 1)
    for i in range(1, cfg.employee_n + 1):
        eid = make_uuid(rng)
        name = f"Employee {i}"
        start_date = base + dt.timedelta(days=(i * 19) % 700)
        work_history = {"role": roles[(i - 1) % len(roles)], "notes": "Seeded employee record"}
        rows.append((eid, name, start_date, work_history))
    return rows


def generate_items_and_inventory(cfg: GenConfig, rng: random.Random):
    milk_opts = ["Whole", "Oat", "Almond", "None"]
    topping_opts = ["Boba", "Lychee Jelly", "Popping Boba", "Chia", "Aloe"]

    item_rows = []
    inv_rows = []
    item_inv_rows = []
    menu_to_item: Dict[int, uuid.UUID] = {}
    item_base_price: Dict[uuid.UUID, float] = {}

    today = cfg.start

    for m in MENU_ITEMS:
        menu_id = int(m["menu_item_id"])
        item_id = make_uuid(rng)
        inv_id = make_uuid(rng)

        name = str(m["name"])
        category = str(m["category"])
        price = float(m["base_price"])
        is_active = bool(m["is_active"])

        milk = milk_opts[menu_id % len(milk_opts)]
        ice = (menu_id * 7) % 3
        sugar = (50 + ((menu_id * 13) % 51)) / 100.0
        toppings = [topping_opts[menu_id % len(topping_opts)]]

        item_rows.append((item_id, name, category, price, is_active, milk, int(ice), float(sugar), toppings))
        menu_to_item[menu_id] = item_id
        item_base_price[item_id] = price

        start_qty = 8000 + ((menu_id * 137) % 2500)
        inv_rows.append((inv_id, int(start_qty), today, today))

        item_inv_rows.append((make_uuid(rng), inv_id, item_id))

    return item_rows, inv_rows, item_inv_rows, menu_to_item, item_base_price


def pick_peak_days(cfg: GenConfig, rng: random.Random) -> List[dt.date]:
    end = cfg.start + dt.timedelta(days=cfg.weeks * 7 - 1)
    all_days = list(daterange(cfg.start, end))

    if cfg.peak_days <= 0:
        return []

    peaks: List[dt.date] = []
    step = len(all_days) / (cfg.peak_days + 1)
    for k in range(1, cfg.peak_days + 1):
        center = int(round(k * step))
        jitter = rng.randint(-5, 5)
        idx = int(clamp(center + jitter, 0, len(all_days) - 1))
        peaks.append(all_days[idx])

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
    item_base_price: Dict[uuid.UUID, float],
):
    """
    Returns:
      order_rows: (order_id, item_quantity_json, employee_id, customer_id, timestamp, total_price)
      order_item_rows: (id, order_id, item_id, quantity, unit_price)
      order_totals: order_id -> total_price
      customer_orders: customer_id -> list[order_id]
      peak_day_list: list[date]
    """
    end = cfg.start + dt.timedelta(days=cfg.weeks * 7 - 1)
    n_days = (end - cfg.start).days + 1

    approx_orders_total = int(max(1, round(cfg.target_sales / cfg.avg_ticket)))
    avg_orders_per_day = approx_orders_total / n_days

    peak_days = pick_peak_days(cfg, rng)
    peak_set = set(peak_days)

    _, _, pop_weights = build_menu_indexes()
    menu_ids = sorted(menu_to_item.keys())
    choice_list = [(mid, pop_weights[mid]) for mid in menu_ids]

    raw: List[Tuple[dt.date, float]] = []
    for d in daterange(cfg.start, end):
        mult = cfg.dow_mult[d.weekday()] * cfg.month_mult[d.month - 1]
        noise = math.exp(rng.gauss(0, cfg.daily_noise_sigma))
        if d in peak_set:
            mult *= cfg.peak_multiplier
        raw.append((d, mult * noise))

    norm = sum(v for _, v in raw) or 1.0

    daily_orders: Dict[dt.date, int] = {}
    for d, v in raw:
        expected = avg_orders_per_day * (v / (norm / n_days))
        lam = max(0.1, expected)
        k = int(max(0, round(rng.gauss(lam, math.sqrt(lam)))))
        daily_orders[d] = k

    order_rows = []
    order_item_rows = []
    order_totals: Dict[uuid.UUID, float] = {}
    customer_orders: Dict[uuid.UUID, List[uuid.UUID]] = {c: [] for c in customers}

    for d, count in daily_orders.items():
        for _ in range(count):
            order_id = make_uuid(rng)

            hour = rng.randint(cfg.open_hour, cfg.close_hour - 1)
            minute = rng.choice([0, 5, 10, 12, 15, 18, 20, 25, 30, 35, 40, 45, 50, 55])
            second = rng.choice([0, 0, 0, 30])
            # Use timezone-aware timestamp (UTC)
            order_dt = dt.datetime(d.year, d.month, d.day, hour, minute, second, tzinfo=dt.timezone.utc)

            customer_id = rng.choice(customers)
            employee_id = rng.choice(employees)

            n_items = weighted_choice(rng, [(1, 0.55), (2, 0.33), (3, 0.12)])

            # Build per-item line items: item_id -> (qty, unit_price)
            line_items: Dict[uuid.UUID, Tuple[int, float]] = {}
            item_qty_json: Dict[str, int] = {}

            for _i in range(n_items):
                mid = weighted_choice(rng, choice_list)
                item_id = menu_to_item[mid]
                qty = weighted_choice(rng, [(1, 0.78), (2, 0.20), (3, 0.02)])

                unit_price = float(item_base_price[item_id])
                unit_price = round(unit_price + rng.choice([0.00, 0.00, 0.25, 0.50, -0.25]), 2)
                unit_price = max(2.50, unit_price)

                prev_qty, prev_price = line_items.get(item_id, (0, unit_price))
                # keep first chosen unit_price for that item in this order
                line_items[item_id] = (prev_qty + qty, prev_price)

            total = 0.0
            for item_id, (qty, unit_price) in line_items.items():
                total += float(unit_price) * int(qty)
                item_qty_json[str(item_id)] = int(qty)
                order_item_rows.append((make_uuid(rng), order_id, item_id, int(qty), float(unit_price)))

            total = round(total, 2)
            order_totals[order_id] = total
            customer_orders[customer_id].append(order_id)

            order_rows.append((order_id, item_qty_json, employee_id, customer_id, order_dt, total))

    # Scale totals to target_sales (bounded)
    current_total = sum(order_totals.values()) or 1.0
    factor = clamp(cfg.target_sales / current_total, 0.75, 1.25)

    if abs(1.0 - factor) > 0.02:
        # Scale both Order.total_price AND Order_Item.unit_price consistently
        new_order_rows = []
        new_order_item_rows = []
        # build order_id -> scaled factor (single global factor)
        for (oid, item_qty, eid, cid, ts, total) in order_rows:
            new_total = round(float(total) * factor, 2)
            order_totals[oid] = new_total
            new_order_rows.append((oid, item_qty, eid, cid, ts, new_total))
        for (rid, oid, item_id, qty, unit_price) in order_item_rows:
            new_price = round(float(unit_price) * factor, 2)
            new_order_item_rows.append((rid, oid, item_id, int(qty), new_price))
        order_rows = new_order_rows
        order_item_rows = new_order_item_rows

    return order_rows, order_item_rows, order_totals, customer_orders, peak_days


def compute_customer_points_and_history(customer_ids, customer_orders, order_totals):
    updates = []
    for cid in customer_ids:
        oids = customer_orders.get(cid, [])
        spend = sum(order_totals.get(oid, 0.0) for oid in oids)
        pts = int(math.floor(spend))
        updates.append((cid, pts, oids))
    return updates


def compute_inventory_after_sales(inv_map_item_to_inventory, order_item_rows, initial_inventory):
    remaining = dict(initial_inventory)
    for (_rid, _oid, item_id, qty, _unit_price) in order_item_rows:
        inv_id = inv_map_item_to_inventory[item_id]
        remaining[inv_id] = max(0, remaining.get(inv_id, 0) - int(qty))
    return remaining


def execute_seed(cfg: GenConfig, dsn: str, truncate_first: bool = False) -> None:
    try:
        import psycopg2
        from psycopg2.extras import execute_values, Json, register_uuid
    except ImportError as e:
        raise SystemExit("psycopg2 is required. Install with: py -m pip install --user psycopg2-binary") from e

    rng = random.Random(cfg.seed)

    customers_full = generate_customers(cfg, rng)
    employees_full = generate_employees(cfg, rng)
    items_full, inv_full, item_inv_full, menu_to_item, item_base_price = generate_items_and_inventory(cfg, rng)

    customer_ids = [c[0] for c in customers_full]
    employee_ids = [e[0] for e in employees_full]

    order_rows, order_item_rows, order_totals, customer_orders, peak_days = generate_orders(
        cfg, rng, customer_ids, employee_ids, menu_to_item, item_base_price
    )

    cust_updates = compute_customer_points_and_history(customer_ids, customer_orders, order_totals)

    inv_map_item_to_inventory = {item_id: inv_id for (_jid, inv_id, item_id) in item_inv_full}
    initial_inventory = {inv_id: qty for (inv_id, qty, _lr, _lq) in inv_full}
    remaining_inventory = compute_inventory_after_sales(inv_map_item_to_inventory, order_item_rows, initial_inventory)

    with psycopg2.connect(dsn) as conn:
        register_uuid(conn_or_curs=conn)
        conn.autocommit = False
        with conn.cursor() as cur:
            if truncate_first:
                cur.execute(
                    """
                    TRUNCATE TABLE
                      "Order_Item",
                      "Item_Inventory",
                      "Order",
                      "Inventory_Quantity",
                      "Item",
                      "Employee",
                      "Customer"
                    CASCADE;
                    """
                )

            execute_values(
                cur,
                """
                INSERT INTO "Customer"(customer_id, name, phone_number, email, points, purchase_history)
                VALUES %s
                """,
                customers_full,
                page_size=1000,
            )

            # psycopg2 Json for work_history
            execute_values(
                cur,
                """
                INSERT INTO "Employee"(employee_id, name, start_date, work_history)
                VALUES %s
                """,
                [(eid, name, sdate, Json(wh)) for (eid, name, sdate, wh) in employees_full],
                page_size=1000,
            )

            execute_values(
                cur,
                """
                INSERT INTO "Item"(item_id, name, category, price, is_active, milk, ice, sugar, toppings)
                VALUES %s
                """,
                items_full,
                page_size=500,
            )

            execute_values(
                cur,
                """
                INSERT INTO "Inventory_Quantity"(inventory_id, quantity, last_restocked, last_quantity)
                VALUES %s
                """,
                inv_full,
                page_size=500,
            )

            execute_values(
                cur,
                """
                INSERT INTO "Item_Inventory"(id, inventory_id, item_id)
                VALUES %s
                """,
                item_inv_full,
                page_size=500,
            )

            execute_values(
                cur,
                """
                INSERT INTO "Order"(order_id, item_quantity, employee_id, customer_id, date, total_price)
                VALUES %s
                """,
                [(oid, Json(item_qty), eid, cid, ts, float(total)) for (oid, item_qty, eid, cid, ts, total) in order_rows],
                page_size=2000,
            )

            execute_values(
                cur,
                """
                INSERT INTO "Order_Item"(id, order_id, item_id, quantity, unit_price)
                VALUES %s
                """,
                order_item_rows,
                page_size=5000,
            )

            execute_values(
                cur,
                """
                UPDATE "Inventory_Quantity" AS iq
                SET quantity = v.quantity
                FROM (VALUES %s) AS v(inventory_id, quantity)
                WHERE iq.inventory_id = v.inventory_id
                """,
                [(inv_id, qty) for inv_id, qty in remaining_inventory.items()],
                page_size=1000,
            )

            execute_values(
                cur,
                """
                UPDATE "Customer" AS c
                SET points = v.points,
                    purchase_history = v.purchase_history
                FROM (VALUES %s) AS v(customer_id, points, purchase_history)
                WHERE c.customer_id = v.customer_id
                """,
                cust_updates,
                page_size=1000,
            )

        conn.commit()

    end = cfg.start + dt.timedelta(days=cfg.weeks * 7 - 1)
    total_sales = sum(order_totals.values())
    print(
        "Seed complete.\n"
        f"  Range:    {cfg.start} .. {end}  ({cfg.weeks} weeks)\n"
        f"  Customers:{len(customers_full)}\n"
        f"  Employees:{len(employees_full)}\n"
        f"  Items:    {len(items_full)}\n"
        f"  Orders:   {len(order_rows)}\n"
        f"  Order_Item rows: {len(order_item_rows)}\n"
        f"  Peak days:{cfg.peak_days}  ({', '.join(str(d) for d in peak_days)})\n"
        f"  Sales ≈   ${total_sales:,.2f}\n"
    )


def parse_args():
    ap = argparse.ArgumentParser()
    ap.add_argument("--start", default=None, help="Start date (YYYY-MM-DD). Default: auto so end is today.")
    ap.add_argument("--weeks", type=int, default=65, help="Weeks to generate (team size 6: 65).")
    ap.add_argument("--seed", type=int, default=42, help="Random seed.")
    ap.add_argument("--target-sales", type=float, default=1_250_000.0, help="Approx total sales for whole range.")
    ap.add_argument("--avg-ticket", type=float, default=10.25, help="Average ticket size.")
    ap.add_argument("--peak-days", type=int, default=4, help="Number of peak days (team size 6: 4).")
    ap.add_argument("--execute", action="store_true", help="Execute inserts into Postgres using --dsn.")
    ap.add_argument("--dsn", default=os.environ.get("PG_DSN", ""), help="psycopg2 DSN string (or set PG_DSN env var).")
    ap.add_argument("--truncate-first", action="store_true", help="TRUNCATE tables before inserting (destructive).")
    return ap.parse_args()


def main():
    args = parse_args()
    weeks = int(args.weeks)

    if args.start:
        start = dt.date.fromisoformat(args.start)
    else:
        start = default_start_for_weeks(weeks)

    cfg = GenConfig(
        start=start,
        weeks=weeks,
        seed=int(args.seed),
        target_sales=float(args.target_sales),
        avg_ticket=float(args.avg_ticket),
        peak_days=int(args.peak_days),
    )

    if not args.execute:
        rng = random.Random(cfg.seed)
        customers = generate_customers(cfg, rng)
        employees = generate_employees(cfg, rng)
        items, inv, item_inv, menu_to_item, item_base_price = generate_items_and_inventory(cfg, rng)
        order_rows, order_item_rows, order_totals, _customer_orders, peak_days = generate_orders(
            cfg, rng,
            [c[0] for c in customers],
            [e[0] for e in employees],
            menu_to_item,
            item_base_price
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
            f"  Peak days: {cfg.peak_days}  ({', '.join(str(d) for d in peak_days)})\n"
            f"  Sales ≈ ${total_sales:,.2f}\n"
        )
        print("\nRun with --execute --dsn '...' to insert into Postgres.")
        return

    if not args.dsn:
        raise SystemExit("--execute requires --dsn (or set PG_DSN env var)")

    execute_seed(cfg, args.dsn, truncate_first=bool(args.truncate_first))


if __name__ == "__main__":
    main()