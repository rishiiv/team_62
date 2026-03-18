SELECT date_trunc('week', o.date) AS week_start,
       SUM(o.total_price) AS week_sales
FROM "Order" o
GROUP BY 1
ORDER BY 1;