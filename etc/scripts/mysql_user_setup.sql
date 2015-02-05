CREATE USER 'TR'@'localhost' IDENTIFIED BY 'TR';
GRANT ALL PRIVILEGES ON tr_rs.* TO 'TR'@'localhost';
GRANT ALL PRIVILEGES ON tr_snaa.* TO 'TR'@'localhost';
GRANT ALL PRIVILEGES ON tr_devicedb.* TO 'TR'@'localhost';
