SELECT i.name,
       SUM(oi.quantity) AS total_qty_sold
FROM "Order_Item" oi
JOIN "Item" i ON i.item_id = oi.item_id
GROUP BY i.name
ORDER BY total_qty_sold DESC
LIMIT 10;