SET storage_engine=INNODB;

DROP DATABASE IF EXISTS tr_snaa;
DROP DATABASE IF EXISTS tr_rs;
DROP DATABASE IF EXISTS tr_devicedb;

CREATE DATABASE tr_snaa;
CREATE DATABASE tr_rs;
CREATE DATABASE tr_devicedb;

DROP USER 'TR'@'localhost';
CREATE USER 'TR'@'localhost' IDENTIFIED BY 'TR';
GRANT ALL PRIVILEGES ON tr_snaa.* TO 'TR'@'localhost';
GRANT ALL PRIVILEGES ON tr_rs.* TO 'TR'@'localhost';
GRANT ALL PRIVILEGES ON tr_devicedb.* TO 'TR'@'localhost';

############################################
##           FILL SNAA DATABASE           ##
############################################

USE tr_snaa;

DROP TABLE IF EXISTS ACTIONS;
DROP TABLE IF EXISTS PERMISSIONS;
DROP TABLE IF EXISTS RESOURCEGROUPS;
DROP TABLE IF EXISTS ROLES;
DROP TABLE IF EXISTS URN_RESOURCEGROUPS;
DROP TABLE IF EXISTS USERS;
DROP TABLE IF EXISTS USERS_ROLES;

CREATE TABLE ACTIONS ( NAME VARCHAR(30) NOT NULL, PRIMARY KEY (NAME) ) ;
CREATE TABLE PERMISSIONS ( ROLE_NAME VARCHAR(150) NOT NULL, ACTION_NAME VARCHAR(30) NOT NULL, RESOURCEGROUP_NAME VARCHAR(40) ) ;
CREATE TABLE RESOURCEGROUPS ( NAME VARCHAR(40) NOT NULL, PRIMARY KEY (NAME) ) ;
CREATE TABLE ROLES ( NAME VARCHAR(150) NOT NULL, PRIMARY KEY (NAME) ) ;
CREATE TABLE URN_RESOURCEGROUPS ( URN VARCHAR(255) NOT NULL, RESOURCEGROUP VARCHAR(40) NOT NULL, PRIMARY KEY (URN, RESOURCEGROUP) ) ;
CREATE TABLE USERS ( NAME VARCHAR(150) NOT NULL, PASSWORD VARCHAR(1500), SALT VARCHAR(1500), PRIMARY KEY (NAME) ) ;
CREATE TABLE USERS_ROLES ( ROLE_NAME VARCHAR(150) NOT NULL, USER_NAME VARCHAR(150) NOT NULL, PRIMARY KEY (ROLE_NAME, USER_NAME) ) ;

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

insert into URN_RESOURCEGROUPS (URN, RESOURCEGROUP) values ('urn:wisebed:uzl2:0x1234', 'EXPERIMENT_ONLY_NODES');
insert into URN_RESOURCEGROUPS (URN, RESOURCEGROUP) values ('urn:wisebed:uzl2:0x2345', 'EXPERIMENT_ONLY_NODES');
insert into URN_RESOURCEGROUPS (URN, RESOURCEGROUP) values ('urn:wisebed:uzl2:0x2065', 'EXPERIMENT_ONLY_NODES');
insert into URN_RESOURCEGROUPS (URN, RESOURCEGROUP) values ('urn:wisebed:uzl2:0x2079', 'EXPERIMENT_ONLY_NODES');

insert into USERS (NAME, PASSWORD, SALT) values ('user1', 'f9731cac4777abcfec4429e10448140fcc22398a4affac6908460bca9d0f73a61d18504337c884340344810b275fef18f9a757cd7ef16b2edd3b854cbdc3192c', 'salt1');
#insert into USERS (NAME, PASSWORD, SALT) values ('user2', '8cd8531f22373bacd6deba1ad56e73d125941767276867169a47c6cdda743e665f4731b0b1545a26f234b54ae32371bd0df98416f39f6ef70454fa0f1116e222', 'salt2');

insert into USERS_ROLES (ROLE_NAME, USER_NAME) values ('ADMINISTRATOR', 'user1');
#insert into USERS_ROLES (ROLE_NAME, USER_NAME) values ('EXPERIMENTER', 'user1');
#insert into USERS_ROLES (ROLE_NAME, USER_NAME) values ('SERVICE_PROVIDER', 'user1');

ALTER TABLE PERMISSIONS ADD CONSTRAINT PERMISSIONS_FK1 FOREIGN KEY (ROLE_NAME) REFERENCES ROLES (NAME);
ALTER TABLE PERMISSIONS ADD CONSTRAINT PERMISSIONS_FK2 FOREIGN KEY (ACTION_NAME) REFERENCES ACTIONS (NAME);
ALTER TABLE PERMISSIONS ADD CONSTRAINT PERMISSIONS_FK3 FOREIGN KEY (RESOURCEGROUP_NAME) REFERENCES RESOURCEGROUPS (NAME);
ALTER TABLE URN_RESOURCEGROUPS ADD CONSTRAINT URN_RESOURCEGROUPS_FK1 FOREIGN KEY (RESOURCEGROUP) REFERENCES RESOURCEGROUPS (NAME);
ALTER TABLE USERS_ROLES ADD CONSTRAINT USERS_ROLES_FK2 FOREIGN KEY (USER_NAME) REFERENCES USERS (NAME);
ALTER TABLE USERS_ROLES ADD CONSTRAINT USERS_ROLES_FK3 FOREIGN KEY (ROLE_NAME) REFERENCES ROLES (NAME);

############################################
##           FILL DEVICE DATABASE         ##
############################################

USE tr_devicedb;

-- MySQL dump 10.13  Distrib 5.5.31, for debian-linux-gnu (i686)
--
-- Host: localhost    Database: tr_devicedb
-- ------------------------------------------------------
-- Server version	5.5.31-0+wheezy1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Capability`
--

DROP TABLE IF EXISTS `Capability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Capability` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `datatype` varchar(255) NOT NULL,
  `defaultValue` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `unit` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Capability`
--

LOCK TABLES `Capability` WRITE;
/*!40000 ALTER TABLE `Capability` DISABLE KEYS */;
/*!40000 ALTER TABLE `Capability` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ChannelHandlerConfig`
--

DROP TABLE IF EXISTS `ChannelHandlerConfig`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ChannelHandlerConfig` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `handlerName` varchar(255) DEFAULT NULL,
  `instanceName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ChannelHandlerConfig`
--

LOCK TABLES `ChannelHandlerConfig` WRITE;
/*!40000 ALTER TABLE `ChannelHandlerConfig` DISABLE KEYS */;
INSERT INTO `ChannelHandlerConfig` VALUES (1,'dlestxetx-framing','dlestxetx-framing'),(2,'dlestxetx-framing','dlestxetx-framing');
/*!40000 ALTER TABLE `ChannelHandlerConfig` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ChannelHandlerConfig_KeyValueEntity`
--

DROP TABLE IF EXISTS `ChannelHandlerConfig_KeyValueEntity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ChannelHandlerConfig_KeyValueEntity` (
  `ChannelHandlerConfig_id` bigint(20) NOT NULL,
  `properties_id` bigint(20) NOT NULL,
  PRIMARY KEY (`ChannelHandlerConfig_id`,`properties_id`),
  KEY `FK7ABC0CEBECDDBC27` (`properties_id`),
  KEY `FK7ABC0CEB6B23A588` (`ChannelHandlerConfig_id`),
  CONSTRAINT `FK7ABC0CEB6B23A588` FOREIGN KEY (`ChannelHandlerConfig_id`) REFERENCES `ChannelHandlerConfig` (`id`),
  CONSTRAINT `FK7ABC0CEBECDDBC27` FOREIGN KEY (`properties_id`) REFERENCES `KeyValueEntity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ChannelHandlerConfig_KeyValueEntity`
--

LOCK TABLES `ChannelHandlerConfig_KeyValueEntity` WRITE;
/*!40000 ALTER TABLE `ChannelHandlerConfig_KeyValueEntity` DISABLE KEYS */;
/*!40000 ALTER TABLE `ChannelHandlerConfig_KeyValueEntity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Coordinate`
--

DROP TABLE IF EXISTS `Coordinate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Coordinate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `phi` double DEFAULT NULL,
  `theta` double DEFAULT NULL,
  `x` double NOT NULL,
  `y` double NOT NULL,
  `z` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Coordinate`
--

LOCK TABLES `Coordinate` WRITE;
/*!40000 ALTER TABLE `Coordinate` DISABLE KEYS */;
/*!40000 ALTER TABLE `Coordinate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `DeviceConfig`
--

DROP TABLE IF EXISTS `DeviceConfig`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DeviceConfig` (
  `nodeUrn` varchar(255) NOT NULL,
  `description` longtext,
  `gatewayNode` tinyint(1) NOT NULL,
  `nodePort` varchar(255) DEFAULT NULL,
  `nodeType` varchar(255) NOT NULL,
  `nodeUSBChipID` varchar(255) DEFAULT NULL,
  `timeoutCheckAliveMillis` bigint(20) DEFAULT NULL,
  `timeoutFlashMillis` bigint(20) DEFAULT NULL,
  `timeoutNodeApiMillis` bigint(20) DEFAULT NULL,
  `timeoutResetMillis` bigint(20) DEFAULT NULL,
  `position_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`nodeUrn`),
  KEY `FKB6E44958766B4057` (`position_id`),
  CONSTRAINT `FKB6E44958766B4057` FOREIGN KEY (`position_id`) REFERENCES `Coordinate` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DeviceConfig`
--

LOCK TABLES `DeviceConfig` WRITE;
/*!40000 ALTER TABLE `DeviceConfig` DISABLE KEYS */;
INSERT INTO `DeviceConfig` VALUES ('urn:wisebed:uzl2:0x1234',NULL,0,NULL,'telosb','XBQTCQYS',3000,120000,1000,3000,NULL),('urn:wisebed:uzl2:0x2065',NULL,0,NULL,'isense48',NULL,3000,120000,1000,3000,NULL),('urn:wisebed:uzl2:0x2079',NULL,0,NULL,'isense48',NULL,3000,120000,1000,3000,NULL),('urn:wisebed:uzl2:0x2345',NULL,0,NULL,'telosb','XBUNEK2E',3000,120000,1000,3000,NULL);
/*!40000 ALTER TABLE `DeviceConfig` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `DeviceConfigEntity_nodeConfiguration`
--

DROP TABLE IF EXISTS `DeviceConfigEntity_nodeConfiguration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DeviceConfigEntity_nodeConfiguration` (
  `DeviceConfigEntity_nodeUrn` varchar(255) NOT NULL,
  `nodeConfiguration` varchar(255) DEFAULT NULL,
  `nodeConfiguration_KEY` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`DeviceConfigEntity_nodeUrn`,`nodeConfiguration_KEY`),
  KEY `FKDBC9EA304E5BFC51` (`DeviceConfigEntity_nodeUrn`),
  CONSTRAINT `FKDBC9EA304E5BFC51` FOREIGN KEY (`DeviceConfigEntity_nodeUrn`) REFERENCES `DeviceConfig` (`nodeUrn`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DeviceConfigEntity_nodeConfiguration`
--

LOCK TABLES `DeviceConfigEntity_nodeConfiguration` WRITE;
/*!40000 ALTER TABLE `DeviceConfigEntity_nodeConfiguration` DISABLE KEYS */;
/*!40000 ALTER TABLE `DeviceConfigEntity_nodeConfiguration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `DeviceConfig_Capability`
--

DROP TABLE IF EXISTS `DeviceConfig_Capability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DeviceConfig_Capability` (
  `DeviceConfig_nodeUrn` varchar(255) NOT NULL,
  `capabilities_id` bigint(20) NOT NULL,
  PRIMARY KEY (`DeviceConfig_nodeUrn`,`capabilities_id`),
  UNIQUE KEY `capabilities_id` (`capabilities_id`),
  KEY `FKA8CF467FFA19A4A` (`capabilities_id`),
  KEY `FKA8CF467F23A5DD6E` (`DeviceConfig_nodeUrn`),
  CONSTRAINT `FKA8CF467F23A5DD6E` FOREIGN KEY (`DeviceConfig_nodeUrn`) REFERENCES `DeviceConfig` (`nodeUrn`),
  CONSTRAINT `FKA8CF467FFA19A4A` FOREIGN KEY (`capabilities_id`) REFERENCES `Capability` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DeviceConfig_Capability`
--

LOCK TABLES `DeviceConfig_Capability` WRITE;
/*!40000 ALTER TABLE `DeviceConfig_Capability` DISABLE KEYS */;
/*!40000 ALTER TABLE `DeviceConfig_Capability` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `DeviceConfig_ChannelHandlerConfig`
--

DROP TABLE IF EXISTS `DeviceConfig_ChannelHandlerConfig`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DeviceConfig_ChannelHandlerConfig` (
  `DeviceConfig_nodeUrn` varchar(255) NOT NULL,
  `defaultChannelPipeline_id` bigint(20) NOT NULL,
  UNIQUE KEY `defaultChannelPipeline_id` (`defaultChannelPipeline_id`),
  KEY `FK3D33A390159CEA4D` (`defaultChannelPipeline_id`),
  KEY `FK3D33A39023A5DD6E` (`DeviceConfig_nodeUrn`),
  CONSTRAINT `FK3D33A39023A5DD6E` FOREIGN KEY (`DeviceConfig_nodeUrn`) REFERENCES `DeviceConfig` (`nodeUrn`),
  CONSTRAINT `FK3D33A390159CEA4D` FOREIGN KEY (`defaultChannelPipeline_id`) REFERENCES `ChannelHandlerConfig` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DeviceConfig_ChannelHandlerConfig`
--

LOCK TABLES `DeviceConfig_ChannelHandlerConfig` WRITE;
/*!40000 ALTER TABLE `DeviceConfig_ChannelHandlerConfig` DISABLE KEYS */;
INSERT INTO `DeviceConfig_ChannelHandlerConfig` VALUES ('urn:wisebed:uzl2:0x2065',2),('urn:wisebed:uzl2:0x2079',1);
/*!40000 ALTER TABLE `DeviceConfig_ChannelHandlerConfig` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `KeyValueEntity`
--

DROP TABLE IF EXISTS `KeyValueEntity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `KeyValueEntity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `key_i` varchar(255) DEFAULT NULL,
  `value_i` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `KeyValueEntity`
--

LOCK TABLES `KeyValueEntity` WRITE;
/*!40000 ALTER TABLE `KeyValueEntity` DISABLE KEYS */;
/*!40000 ALTER TABLE `KeyValueEntity` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-07-18 11:21:11

USE tr_rs;

-- MySQL dump 10.13  Distrib 5.5.31, for debian-linux-gnu (i686)
--
-- Host: localhost    Database: tr_rs
-- ------------------------------------------------------
-- Server version	5.5.31-0+wheezy1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ConfidentialReservationDataInternal`
--

DROP TABLE IF EXISTS `ConfidentialReservationDataInternal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ConfidentialReservationDataInternal` (
  `description` varchar(255) DEFAULT NULL,
  `secretReservationKey` varchar(255) DEFAULT NULL,
  `urnPrefix` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1BAA03ABA4EF0E2F` (`id`),
  CONSTRAINT `FK1BAA03ABA4EF0E2F` FOREIGN KEY (`id`) REFERENCES `PublicReservationDataInternal` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ConfidentialReservationDataInternal`
--

LOCK TABLES `ConfidentialReservationDataInternal` WRITE;
/*!40000 ALTER TABLE `ConfidentialReservationDataInternal` DISABLE KEYS */;
/*!40000 ALTER TABLE `ConfidentialReservationDataInternal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ConfidentialReservationDataInternal_options`
--

DROP TABLE IF EXISTS `ConfidentialReservationDataInternal_options`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ConfidentialReservationDataInternal_options` (
  `ConfidentialReservationDataInternal_id` bigint(20) NOT NULL,
  `options` varchar(255) DEFAULT NULL,
  `options_KEY` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`ConfidentialReservationDataInternal_id`,`options_KEY`),
  KEY `FKEAC3560AFD4229C4` (`ConfidentialReservationDataInternal_id`),
  CONSTRAINT `FKEAC3560AFD4229C4` FOREIGN KEY (`ConfidentialReservationDataInternal_id`) REFERENCES `ConfidentialReservationDataInternal` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ConfidentialReservationDataInternal_options`
--

LOCK TABLES `ConfidentialReservationDataInternal_options` WRITE;
/*!40000 ALTER TABLE `ConfidentialReservationDataInternal_options` DISABLE KEYS */;
/*!40000 ALTER TABLE `ConfidentialReservationDataInternal_options` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `PublicReservationDataInternal`
--

DROP TABLE IF EXISTS `PublicReservationDataInternal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PublicReservationDataInternal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fromDate` bigint(20) NOT NULL,
  `toDate` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `PublicReservationDataInternal`
--

LOCK TABLES `PublicReservationDataInternal` WRITE;
/*!40000 ALTER TABLE `PublicReservationDataInternal` DISABLE KEYS */;
/*!40000 ALTER TABLE `PublicReservationDataInternal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ReservationDataInternal`
--

DROP TABLE IF EXISTS `ReservationDataInternal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReservationDataInternal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `deleted` tinyint(1) DEFAULT NULL,
  `urnPrefix` varchar(255) DEFAULT NULL,
  `confidentialReservationData_id` bigint(20) DEFAULT NULL,
  `secretReservationKey_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9B25EE53C8BC37A1` (`confidentialReservationData_id`),
  KEY `FK9B25EE536D55862D` (`secretReservationKey_id`),
  CONSTRAINT `FK9B25EE536D55862D` FOREIGN KEY (`secretReservationKey_id`) REFERENCES `SecretReservationKeyInternal` (`id`),
  CONSTRAINT `FK9B25EE53C8BC37A1` FOREIGN KEY (`confidentialReservationData_id`) REFERENCES `ConfidentialReservationDataInternal` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ReservationDataInternal`
--

LOCK TABLES `ReservationDataInternal` WRITE;
/*!40000 ALTER TABLE `ReservationDataInternal` DISABLE KEYS */;
/*!40000 ALTER TABLE `ReservationDataInternal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SecretReservationKeyInternal`
--

DROP TABLE IF EXISTS `SecretReservationKeyInternal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SecretReservationKeyInternal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `secretReservationKey` varchar(255) NOT NULL,
  `urnPrefix` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `secretReservationKey` (`secretReservationKey`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SecretReservationKeyInternal`
--

LOCK TABLES `SecretReservationKeyInternal` WRITE;
/*!40000 ALTER TABLE `SecretReservationKeyInternal` DISABLE KEYS */;
/*!40000 ALTER TABLE `SecretReservationKeyInternal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservationdata_urns`
--

DROP TABLE IF EXISTS `reservationdata_urns`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reservationdata_urns` (
  `urn_id` bigint(20) NOT NULL,
  `urns` varchar(255) NOT NULL,
  `POSITION` int(11) NOT NULL,
  PRIMARY KEY (`urn_id`,`POSITION`),
  KEY `FK8A442E4B730FEBBD` (`urn_id`),
  CONSTRAINT `FK8A442E4B730FEBBD` FOREIGN KEY (`urn_id`) REFERENCES `PublicReservationDataInternal` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservationdata_urns`
--

LOCK TABLES `reservationdata_urns` WRITE;
/*!40000 ALTER TABLE `reservationdata_urns` DISABLE KEYS */;
/*!40000 ALTER TABLE `reservationdata_urns` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-07-18 11:28:54
