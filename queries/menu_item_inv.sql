SELECT m.menu_item_id, m.name, 
COUNT(mi.inventory_item_id) 
AS ingredient_item_count 
FROM menu_items m JOIN menu_item_ingredients mi ON mi.menu_item_id = m.menu_item_id 
GROUP BY m.menu_item_id, m.name 
ORDER BY ingredient_item_count DESC, m.name;