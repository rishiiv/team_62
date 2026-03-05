SELECT COUNT(DISTINCT date_trunc('week', o.date)) AS distinct_weeks
FROM "Order" o;