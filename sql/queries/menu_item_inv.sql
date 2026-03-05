SELECT i.name AS menu_item,
       iq.quantity AS inventory_quantity
FROM "Item" i
JOIN "Item_Inventory" ii ON ii.item_id = i.item_id
JOIN "Inventory_Quantity" iq ON iq.inventory_id = ii.inventory_id
ORDER BY i.name;