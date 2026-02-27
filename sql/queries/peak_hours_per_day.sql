WITH hourly AS (
  SELECT o.date::date AS day,
         EXTRACT(HOUR FROM o.date) AS hour_of_day,
         COUNT(*) AS order_count
  FROM "Order" o
  GROUP BY 1, 2
),
ranked AS (
  SELECT *,
         ROW_NUMBER() OVER (PARTITION BY day ORDER BY order_count DESC, hour_of_day ASC) AS rn
  FROM hourly
)
SELECT day, hour_of_day AS busiest_hour, order_count
FROM ranked
WHERE rn = 1
ORDER BY day;