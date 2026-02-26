SELECT DATE_TRUNC('hour', order_datetime) 
AS hour_bucket, 
COUNT(*) 
AS order_count, 
SUM(total_amount) 
AS total_sales 
FROM sales_orders GROUP BY 1 ORDER BY 1;
