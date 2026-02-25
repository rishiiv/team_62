SELECT DATE(order_datetime) 
AS day, SUM(total_amount) 
AS total_sales 
FROM sales_orders 
GROUP BY 1 
ORDER BY total_sales 
DESC LIMIT 10;
