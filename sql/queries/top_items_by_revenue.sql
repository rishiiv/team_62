SELECT i.name,
       SUM(oi.quantity * oi.unit_price) AS revenue
FROM "Order_Item" oi
JOIN "Item" i ON i.item_id = oi.item_id
GROUP BY i.name
ORDER BY revenue DESC
LIMIT 10;