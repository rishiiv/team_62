WITH day_sales AS (
  SELECT date_trunc('week', o.date)::date AS week_start,
         o.date::date AS day,
         SUM(o.total_price) AS day_sales
  FROM "Order" o
  GROUP BY 1, 2
),
worst_day AS (
  SELECT DISTINCT ON (week_start)
         week_start,
         day AS worst_day,
         day_sales AS worst_day_sales
  FROM day_sales
  ORDER BY week_start, day_sales ASC
),
item_sales_on_worst AS (
  SELECT wd.week_start,
         wd.worst_day,
         i.name AS item_name,
         SUM(oi.quantity) AS qty_sold
  FROM worst_day wd
  JOIN "Order" o ON o.date::date = wd.worst_day
  JOIN "Order_Item" oi ON oi.order_id = o.order_id
  JOIN "Item" i ON i.item_id = oi.item_id
  GROUP BY wd.week_start, wd.worst_day, i.name
),
top_item AS (
  SELECT DISTINCT ON (week_start)
         week_start, worst_day, item_name, qty_sold
  FROM item_sales_on_worst
  ORDER BY week_start, qty_sold DESC, item_name
)
SELECT wd.week_start,
       wd.worst_day AS lowest_sales_day,
       wd.worst_day_sales AS lowest_day_sales,
       ti.item_name AS top_seller_item,
       ti.qty_sold AS top_seller_qty
FROM worst_day wd
LEFT JOIN top_item ti USING (week_start, worst_day)
ORDER BY wd.week_start;