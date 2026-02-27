SELECT date_trunc('month', o.date) AS month_start,
       COUNT(*) AS orders,
       AVG(o.total_price) AS avg_ticket,
       SUM(o.total_price) AS sales
FROM "Order" o
GROUP BY 1
ORDER BY 1;