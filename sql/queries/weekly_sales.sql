SELECT date_trunc('week', o.date) AS week_start,
       COUNT(*) AS order_count
FROM "Order" o
GROUP BY 1
ORDER BY 1;