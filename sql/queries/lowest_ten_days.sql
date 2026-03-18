SELECT o.date::date AS day,
       SUM(o.total_price) AS total_sales
FROM "Order" o
GROUP BY 1
ORDER BY total_sales ASC
LIMIT 10;