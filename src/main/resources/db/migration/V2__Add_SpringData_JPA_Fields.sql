-- Flyway Migration V2: Add Spring Data JPA Fields
-- This migration adds version field for optimistic locking support in Spring Data JPA

-- Add version column to users table for optimistic locking
ALTER TABLE `users` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `email`;

-- Add version column to accounts table for optimistic locking
ALTER TABLE `accounts` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `refresh`;

-- Add version column to articles table for optimistic locking
ALTER TABLE `articles` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `data`;

-- Add version column to auditorium_talks table for optimistic locking
ALTER TABLE `auditorium_talks` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `description`;

-- Add version column to bullets table for optimistic locking
ALTER TABLE `bullets` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `message`;

-- Add version column to emails table for optimistic locking
ALTER TABLE `emails` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `read_ind`;

-- Add version column to entry_types table for optimistic locking
ALTER TABLE `entry_types` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `last_update`;

-- Add version column to gateways table for optimistic locking
ALTER TABLE `gateways` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `active`;

-- Add version column to menu_item_entries table for optimistic locking
ALTER TABLE `menu_item_entries` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `menu_path`;

-- Add version column to qfiles table for optimistic locking
ALTER TABLE `qfiles` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `description`;

-- Add version column to room_logs table for optimistic locking
ALTER TABLE `room_logs` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `duration`;

-- Add version column to toc_entries table for optimistic locking
ALTER TABLE `toc_entries` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `create_date`;

-- Add version column to vendor_rooms table for optimistic locking
ALTER TABLE `vendor_rooms` 
ADD COLUMN `version` int(11) NOT NULL default '0' AFTER `active`;