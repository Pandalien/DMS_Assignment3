SELECT * FROM user;

SELECT * FROM transaction;

SELECT * FROM user u, transaction t WHERE t.driver_id = u.user_id;