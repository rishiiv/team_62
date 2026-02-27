WITH item_sales AS (
  SELECT i.category, i.name, SUM(oi.quantity) AS qty_sold
  FROM "Order_Item" oi
  JOIN "Item" i ON i.item_id = oi.item_id
  GROUP BY i.category, i.name
),
ranked AS (
  SELECT *, ROW_NUMBER() OVER (PARTITION BY category ORDER BY qty_sold DESC, name ASC) AS rn
  FROM item_sales
)
SELECT category, name AS top_item, qty_sold
FROM ranked
WHERE rn = 1
ORDER BY category;