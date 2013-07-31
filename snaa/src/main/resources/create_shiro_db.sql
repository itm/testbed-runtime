DROP DATABASE IF EXISTS trauth;
CREATE DATABASE IF NOT EXISTS trauth;
USE trauth;

-- this is necessary when using mysql to enable foreign keys
SET storage_engine=INNODB;

-- remove the comments of the following two lines when using derby in memory data base
-- connect 'jdbc:derby:memory:derbyDB';
-- use enterprise_it_ep00;

DROP TABLE IF EXISTS ACTIONS;
DROP TABLE IF EXISTS PERMISSIONS;
DROP TABLE IF EXISTS RESOURCEGROUPS;
DROP TABLE IF EXISTS ROLES;
DROP TABLE IF EXISTS URN_RESOURCEGROUPS;
DROP TABLE IF EXISTS USERS;
DROP TABLE IF EXISTS USERS_ROLES;
DROP TABLE IF EXISTS USERS_CERT;
DROP TABLE IF EXISTS USERSCERT_ROLES;
DROP TABLE IF EXISTS ORGANIZATION;

CREATE TABLE ACTIONS ( NAME VARCHAR(30) NOT NULL, PRIMARY KEY (NAME) ) ;
CREATE TABLE PERMISSIONS ( ROLE_NAME VARCHAR(150) NOT NULL, ACTION_NAME VARCHAR(30) NOT NULL, RESOURCEGROUP_NAME VARCHAR(40) ) ;
CREATE TABLE RESOURCEGROUPS ( NAME VARCHAR(40) NOT NULL, PRIMARY KEY (NAME) ) ;
CREATE TABLE ROLES ( NAME VARCHAR(150) NOT NULL, PRIMARY KEY (NAME) ) ;
CREATE TABLE URN_RESOURCEGROUPS ( URN VARCHAR(255) NOT NULL, RESOURCEGROUP VARCHAR(40) NOT NULL, PRIMARY KEY (URN, RESOURCEGROUP) ) ;
CREATE TABLE USERS ( NAME VARCHAR(150) NOT NULL, PASSWORD VARCHAR(1500), SALT VARCHAR(1500), PRIMARY KEY (NAME) ) ;
CREATE TABLE USERS_ROLES ( ROLE_NAME VARCHAR(150) NOT NULL, USER_NAME VARCHAR(150) NOT NULL, PRIMARY KEY (ROLE_NAME, USER_NAME) ) ;
CREATE TABLE USERS_CERT ( NAME VARCHAR(150) NOT NULL, ORGANIZATIONID VARCHAR(150) NOT NULL, HASCERTIFICATE BOOLEAN NOT NULL, PRIMARY KEY (NAME) );
CREATE TABLE USERSCERT_ROLES ( ROLE_NAME VARCHAR(150) NOT NULL, USERSCERT_NAME VARCHAR(150) NOT NULL, PRIMARY KEY (ROLE_NAME,USERSCERT_NAME) ) ;
CREATE TABLE ORGANIZATION( NAME VARCHAR(50) NOT NULL, URL VARCHAR(150) NOT NULL, PRIMARY KEY (NAME));

insert into ACTIONS (NAME) values ('RS_DELETE_RESERVATION');
insert into ACTIONS (NAME) values ('RS_GET_RESERVATIONS');
insert into ACTIONS (NAME) values ('RS_MAKE_RESERVATION');
insert into ACTIONS (NAME) values ('SM_ARE_NODES_ALIVE');
insert into ACTIONS (NAME) values ('SM_FREE');
insert into ACTIONS (NAME) values ('WSN_ARE_NODES_ALIVE');
insert into ACTIONS (NAME) values ('WSN_DESTROY_VIRTUAL_LINK');
insert into ACTIONS (NAME) values ('WSN_DISABLE_NODE');
insert into ACTIONS (NAME) values ('WSN_DISABLE_PHYSICAL_LINK');
insert into ACTIONS (NAME) values ('WSN_ENABLE_NODE');
insert into ACTIONS (NAME) values ('WSN_ENABLE_PHYSICAL_LINK');
insert into ACTIONS (NAME) values ('WSN_FLASH_PROGRAMS');
insert into ACTIONS (NAME) values ('WSN_RESET_NODES');
insert into ACTIONS (NAME) values ('WSN_SEND');
insert into ACTIONS (NAME) values ('WSN_SET_CHANNEL_PIPELINE');
insert into ACTIONS (NAME) values ('WSN_SET_VIRTUAL_LINK');
insert into ACTIONS (NAME) values ('WSN_SUBSCRIBE');
insert into ACTIONS (NAME) values ('*');

insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('ADMINISTRATOR', '*', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('ADMINISTRATOR', '*', 'SERVICE_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('ADMINISTRATOR', '*', 'SERVICE_AND_EXPERIMENT_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'RS_DELETE_RESERVATION', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'RS_DELETE_RESERVATION', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'RS_GET_RESERVATIONS', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'RS_MAKE_RESERVATION', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'SM_ARE_NODES_ALIVE', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'SM_FREE', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_ARE_NODES_ALIVE', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_DESTROY_VIRTUAL_LINK', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_DISABLE_NODE', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_DISABLE_PHYSICAL_LINK', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_ENABLE_NODE', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_ENABLE_PHYSICAL_LINK', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_FLASH_PROGRAMS', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_RESET_NODES', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_SEND', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_SET_CHANNEL_PIPELINE', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('EXPERIMENTER', 'WSN_SET_VIRTUAL_LINK', 'EXPERIMENT_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('SERVICE_PROVIDER', 'RS_DELETE_RESERVATION', 'SERVICE_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('SERVICE_PROVIDER', 'RS_MAKE_RESERVATION', 'SERVICE_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('SERVICE_PROVIDER', 'SM_ARE_NODES_ALIVE', 'SERVICE_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('SERVICE_PROVIDER', 'WSN_ARE_NODES_ALIVE', 'SERVICE_ONLY_NODES');
insert into PERMISSIONS (ROLE_NAME, ACTION_NAME, RESOURCEGROUP_NAME) values ('SERVICE_PROVIDER', 'WSN_SEND', 'SERVICE_ONLY_NODES');

insert into RESOURCEGROUPS (NAME) values ('EXPERIMENT_ONLY_NODES');
insert into RESOURCEGROUPS (NAME) values ('SERVICE_ONLY_NODES');
insert into RESOURCEGROUPS (NAME) values ('SERVICE_AND_EXPERIMENT_NODES');
insert into RESOURCEGROUPS (NAME) values ('*');

insert into ROLES (NAME) values ('ADMINISTRATOR');
insert into ROLES (NAME) values ('EXPERIMENTER');
insert into ROLES (NAME) values ('SERVICE_PROVIDER');

insert into URN_RESOURCEGROUPS (URN, RESOURCEGROUP) values ('urn:wisebed:uzl1:0x1234', 'EXPERIMENT_ONLY_NODES');
insert into URN_RESOURCEGROUPS (URN, RESOURCEGROUP) values ('urn:wisebed:uzl1:0x2345', 'EXPERIMENT_ONLY_NODES');
insert into URN_RESOURCEGROUPS (URN, RESOURCEGROUP) values ('urn:wisebed:uzl1:0x3456', 'EXPERIMENT_ONLY_NODES');

insert into USERS (NAME, PASSWORD, SALT) values ('user1', 'f9731cac4777abcfec4429e10448140fcc22398a4affac6908460bca9d0f73a61d18504337c884340344810b275fef18f9a757cd7ef16b2edd3b854cbdc3192c', 'salt1');
insert into USERS (NAME, PASSWORD, SALT) values ('user2', '8cd8531f22373bacd6deba1ad56e73d125941767276867169a47c6cdda743e665f4731b0b1545a26f234b54ae32371bd0df98416f39f6ef70454fa0f1116e222', 'salt2');

insert into USERS_ROLES (ROLE_NAME, USER_NAME) values ('ADMINISTRATOR', 'user1');
insert into USERS_ROLES (ROLE_NAME, USER_NAME) values ('EXPERIMENTER', 'user1');
insert into USERS_ROLES (ROLE_NAME, USER_NAME) values ('SERVICE_PROVIDER', 'user1');

insert into USERS_CERT (NAME, ORGANIZATIONID, HASCERTIFICATE) values ('Administrator1', 'Organization1', 1);
insert into USERS_CERT (NAME, ORGANIZATIONID, HASCERTIFICATE) values ('Experimenter1', 'Organization1', 1);
insert into USERS_CERT (NAME, ORGANIZATIONID, HASCERTIFICATE) values ('ServiceProvider1', 'Organization1', 1);

insert into USERSCERT_ROLES (ROLE_NAME, USERSCERT_NAME) values ('ADMINISTRATOR', 'Administrator1');
insert into USERSCERT_ROLES (ROLE_NAME, USERSCERT_NAME) values ('EXPERIMENTER', 'Experimenter1');
insert into USERSCERT_ROLES (ROLE_NAME, USERSCERT_NAME) values ('SERVICE_PROVIDER', 'ServiceProvider1');

INSERT INTO ORGANIZATION(NAME, URL) VALUES ("Organization1","Organization Url");

ALTER TABLE PERMISSIONS ADD CONSTRAINT PERMISSIONS_FK1 FOREIGN KEY (ROLE_NAME) REFERENCES ROLES (NAME);
ALTER TABLE PERMISSIONS ADD CONSTRAINT PERMISSIONS_FK2 FOREIGN KEY (ACTION_NAME) REFERENCES ACTIONS (NAME);
ALTER TABLE PERMISSIONS ADD CONSTRAINT PERMISSIONS_FK3 FOREIGN KEY (RESOURCEGROUP_NAME) REFERENCES RESOURCEGROUPS (NAME);
ALTER TABLE URN_RESOURCEGROUPS ADD CONSTRAINT URN_RESOURCEGROUPS_FK1 FOREIGN KEY (RESOURCEGROUP) REFERENCES RESOURCEGROUPS (NAME);
ALTER TABLE USERS_ROLES ADD CONSTRAINT USERS_ROLES_FK2 FOREIGN KEY (USER_NAME) REFERENCES USERS (NAME);
ALTER TABLE USERS_ROLES ADD CONSTRAINT USERS_ROLES_FK3 FOREIGN KEY (ROLE_NAME) REFERENCES ROLES (NAME);
ALTER TABLE USERS_CERT ADD CONSTRAINT USERS_CERT_FK1 FOREIGN KEY (ORGANIZATIONID) REFERENCES ORGANIZATION (NAME);
ALTER TABLE USERSCERT_ROLES ADD CONSTRAINT USERSCERT_ROLES_FK3 FOREIGN KEY (ROLE_NAME) REFERENCES ROLES (NAME);
ALTER TABLE USERSCERT_ROLES ADD CONSTRAINT USERSCERT_ROLES_FK4 FOREIGN KEY (USERSCERT_NAME) REFERENCES USERS_CERT (NAME);


DROP USER 'trauth'@'localhost';
CREATE USER 'trauth'@'localhost' IDENTIFIED BY 'trauth';
GRANT ALL PRIVILEGES ON trauth.* TO 'trauth'@'localhost';

-- remove the comments of the following two lines when using derby in memory data base
-- commit;
-- disconnect all;
