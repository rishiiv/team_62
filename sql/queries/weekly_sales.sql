
SELECT DATE_TRUNC('week', order_datetime) 
AS week_start, COUNT(*) 
AS order_count 
FROM sales_orders 
GROUP BY 1 
ORDER BY 1;