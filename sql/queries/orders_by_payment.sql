WITH pm AS (
  SELECT order_id,
         CASE (('x' || substr(md5(order_id::text), 1, 8))::bit(32)::int % 3)
           WHEN 0 THEN 'Card'
           WHEN 1 THEN 'Cash'
           ELSE 'Mobile'
         END AS payment_method,
         total_price
  FROM "Order"
)
SELECT payment_method,
       COUNT(*) AS orders,
       SUM(total_price) AS sales
FROM pm
GROUP BY 1
ORDER BY sales DESC;