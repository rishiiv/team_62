SELECT date_trunc('hour', o.date) AS hour_bucket,
       COUNT(*) AS order_count,
       SUM(o.total_price) AS total_sales
FROM "Order" o
GROUP BY 1
ORDER BY 1;