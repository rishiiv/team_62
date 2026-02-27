SELECT i.name AS item_name,
       iq.quantity,
       CASE
         WHEN iq.quantity < 200 THEN 'LOW'
         WHEN iq.quantity < 800 THEN 'MEDIUM'
         ELSE 'OK'
       END AS stock_status
FROM "Inventory_Quantity" iq
JOIN "Item_Inventory" ii ON ii.inventory_id = iq.inventory_id
JOIN "Item" i ON i.item_id = ii.item_id
ORDER BY iq.quantity ASC
LIMIT 15;