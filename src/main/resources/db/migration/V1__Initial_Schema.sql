-- Flyway Migration V1: Initial Schema
-- This migration creates the core tables for QLinkServer

-- Create users table
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `access_code` char(20) NOT NULL default '',
  `active` enum('N','Y') NOT NULL default 'N',
  `create_date` datetime NOT NULL default '1990-04-13 00:00:00',
  `last_access` datetime NOT NULL default '1990-04-13 00:00:00',
  `last_update` datetime NOT NULL default '1990-04-13 00:00:00',
  `orig_account` char(20) NOT NULL default '',
  `orig_code` char(20) NOT NULL default '',
  `name` varchar(50) NOT NULL default '',
  `city` varchar(50) NOT NULL default '',
  `state` char(2) NOT NULL default '',
  `country` varchar(50) NOT NULL default '',
  `email` varchar(100) NOT NULL default '',
  PRIMARY KEY  (`user_id`),
  UNIQUE KEY `access_code` (`access_code`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- Create accounts table
CREATE TABLE IF NOT EXISTS `accounts` (
  `account_id` int(11) NOT NULL AUTO_INCREMENT,
  `primary_ind` enum('Y','N') NOT NULL default 'Y',
  `staff_ind` enum('N','Y') NOT NULL default 'N',
  `user_id` int(11) NOT NULL default '0',
  `active` enum('N','Y') NOT NULL default 'N',
  `handle` char(10) NOT NULL default '',
  `create_date` datetime NOT NULL default '1990-04-13 00:00:00',
  `last_access` datetime NOT NULL default '1990-04-13 00:00:00',
  `last_update` datetime NOT NULL default '1990-04-13 00:00:00',
  `refresh` enum('N','Y') NOT NULL default 'N',
  PRIMARY KEY  (`account_id`),
  UNIQUE KEY `handle` (`handle`),
  KEY `user_id` (`user_id`),
  KEY `primary_ind` (`primary_ind`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- Create articles table
CREATE TABLE IF NOT EXISTS `articles` (
  `article_id` int(11) NOT NULL default '0',
  `next_id` int(11) NOT NULL default '0',
  `prev_id` int(11) NOT NULL default '0',
  `data` text,
  PRIMARY KEY  (`article_id`),
  KEY `next_id` (`next_id`,`prev_id`)
) ENGINE=InnoDB;

-- Create auditorium_talks table
CREATE TABLE IF NOT EXISTS `auditorium_talks` (
  `talk_id` int(11) NOT NULL auto_increment,
  `title` varchar(100) NOT NULL default '',
  `speaker` varchar(50) NOT NULL default '',
  `start_time` datetime NOT NULL default '1990-04-13 00:00:00',
  `end_time` datetime NOT NULL default '1990-04-13 00:00:00',
  `description` text,
  PRIMARY KEY  (`talk_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- Create bullets table
CREATE TABLE IF NOT EXISTS `bullets` (
  `bullet_id` int(11) NOT NULL default '0',
  `message` text,
  `create_date` datetime NOT NULL default '1990-04-13 00:00:00',
  `user_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`bullet_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB;

-- Create emails table
CREATE TABLE IF NOT EXISTS `emails` (
  `email_id` int(11) NOT NULL auto_increment,
  `from_user_id` int(11) NOT NULL default '0',
  `to_user_id` int(11) NOT NULL default '0',
  `subject` varchar(100) NOT NULL default '',
  `message` text,
  `create_date` datetime NOT NULL default '1990-04-13 00:00:00',
  `read_ind` enum('N','Y') NOT NULL default 'N',
  PRIMARY KEY  (`email_id`),
  KEY `from_user_id` (`from_user_id`),
  KEY `to_user_id` (`to_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- Create entry_types table
CREATE TABLE IF NOT EXISTS `entry_types` (
  `entry_type_id` int(11) NOT NULL default '0',
  `max_entry` int(11) NOT NULL default '0',
  `type` varchar(20) NOT NULL default '',
  `premium_ind` enum('N','Y') NOT NULL default 'N',
  `create_date` datetime NOT NULL default '1990-04-13 00:00:00',
  `last_update` datetime NOT NULL default '1990-04-13 00:00:00',
  PRIMARY KEY  (`entry_type_id`)
) ENGINE=InnoDB;

-- Create gateways table
CREATE TABLE IF NOT EXISTS `gateways` (
  `gateway_id` int(11) NOT NULL auto_increment,
  `gateway_name` varchar(50) NOT NULL default '',
  `gateway_type` varchar(20) NOT NULL default '',
  `host` varchar(50) NOT NULL default '',
  `port` int(11) NOT NULL default '0',
  `active` enum('N','Y') NOT NULL default 'N',
  PRIMARY KEY  (`gateway_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- Create menu_item_entries table
CREATE TABLE IF NOT EXISTS `menu_item_entries` (
  `menu_item_id` int(11) NOT NULL default '0',
  `entry_id` int(11) NOT NULL default '0',
  `entry_type_id` int(11) NOT NULL default '0',
  `menu_path` varchar(100) NOT NULL default '',
  PRIMARY KEY  (`menu_item_id`,`entry_id`)
) ENGINE=InnoDB;

-- Create qfiles table
CREATE TABLE IF NOT EXISTS `qfiles` (
  `file_id` int(11) NOT NULL auto_increment,
  `filename` varchar(100) NOT NULL default '',
  `file_size` int(11) NOT NULL default '0',
  `file_date` datetime NOT NULL default '1990-04-13 00:00:00',
  `uploader_id` int(11) NOT NULL default '0',
  `entry_type_id` int(11) NOT NULL default '0',
  `download_count` int(11) NOT NULL default '0',
  `description` text,
  PRIMARY KEY  (`file_id`),
  KEY `uploader_id` (`uploader_id`),
  KEY `entry_type_id` (`entry_type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- Create room_logs table
CREATE TABLE IF NOT EXISTS `room_logs` (
  `log_id` int(11) NOT NULL auto_increment,
  `user_id` int(11) NOT NULL default '0',
  `room_name` varchar(50) NOT NULL default '',
  `login_time` datetime NOT NULL default '1990-04-13 00:00:00',
  `logout_time` datetime NOT NULL default '1990-04-13 00:00:00',
  `duration` int(11) NOT NULL default '0',
  PRIMARY KEY  (`log_id`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- Create toc_entries table
CREATE TABLE IF NOT EXISTS `toc_entries` (
  `toc_id` int(11) NOT NULL auto_increment,
  `toc_name` varchar(50) NOT NULL default '',
  `entry_type_id` int(11) NOT NULL default '0',
  `create_date` datetime NOT NULL default '1990-04-13 00:00:00',
  PRIMARY KEY  (`toc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- Create vendor_rooms table
CREATE TABLE IF NOT EXISTS `vendor_rooms` (
  `vendor_id` int(11) NOT NULL auto_increment,
  `vendor_name` varchar(50) NOT NULL default '',
  `room_name` varchar(50) NOT NULL default '',
  `description` text,
  `active` enum('N','Y') NOT NULL default 'N',
  PRIMARY KEY  (`vendor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;