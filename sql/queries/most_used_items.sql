SELECT i.name,
       SUM(oi.quantity) AS total_used
FROM "Order_Item" oi
JOIN "Item" i ON i.item_id = oi.item_id
GROUP BY i.name
ORDER BY total_used DESC
LIMIT 25;