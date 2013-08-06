DROP DATABASE IF EXISTS tr_organization;
CREATE DATABASE IF NOT EXISTS tr_organization;
USE tr_organization;

-- this is necessary when using mysql to enable foreign keys
SET storage_engine=INNODB;

CREATE TABLE USERS (
    userId varchar(30),
    userRole varchar(20),
    organisationId varchar(20)
);


INSERT INTO USERS(userId, userRole, organisationId) VALUES("Administrator1","ADMINISTRATOR", "Organization1");

INSERT INTO USERS(userId, userRole, organisationId) VALUES("Experimenter1", "EXPERIMENTER", "Organization1");

INSERT INTO USERS(userId, userRole, organisationId) VALUES("ServiceProvider1", "SERVICE_PROVIDER", "Organization1");


DROP USER 'trorg'@'localhost';
CREATE USER 'trorg'@'localhost' IDENTIFIED BY 'trorg';
GRANT ALL PRIVILEGES ON tr_organization.* TO 'trorg'@'localhost';