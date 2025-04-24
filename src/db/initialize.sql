-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema hktv_ars
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `hktv_ars` DEFAULT CHARACTER SET utf8 ;
USE `hktv_ars`;

-- -----------------------------------------------------
-- Table `hktv_ars`.`system_variable`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_variable` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `value` VARCHAR(64) NOT NULL,
  `description` VARCHAR(255) NULL,
  `visible` TINYINT(1) DEFAULT TRUE,
  `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by`         BIGINT        NOT NULL,
  `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `last_modified_by`         BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `system_variable_name_idx_UNIQUE` (`name` ASC) VISIBLE)
ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `hktv_ars`.`cron_job_log`
-- -----------------------------------------------------
CREATE TABLE `hktv_ars`.`cron_job_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `job_name` VARCHAR(255) NOT NULL,
  `job_group` VARCHAR(255) NOT NULL,
  `status` VARCHAR(50) NOT NULL,
  `error_message` TEXT,
  `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `cron_job_log_job_name_job_group_idx1` (`job_name` ASC, `job_group` ASC) VISIBLE)
ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `hktv_ars`.`sso_user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sso_user`
(
    `id`                 BIGINT NOT NULL AUTO_INCREMENT,
    `user_uuid`          VARCHAR(36) NOT NULL,
    `user_name`          varchar(45) NOT NULL,
    `creation_date`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME    NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `sso_user_user_name_idx_UNIQUE` (`user_name` ASC) VISIBLE,
    UNIQUE INDEX `sso_user_user_uuid_idx_UNIQUE` (`user_uuid` ASC) VISIBLE
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `hktv_ars`.`auth_security_role`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `auth_security_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `auth_security_role_name_idx_UNIQUE` (`name` ASC) VISIBLE)
ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `hktv_ars`.`auth_security_api`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `auth_security_api` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `url` VARCHAR(100),
    `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `auth_security_api_url_idx_UNIQUE` (`url` ASC) VISIBLE)
ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `hktv_ars`.`auth_security_api_role`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `auth_security_api_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `auth_security_api_id` BIGINT NOT NULL,
    `auth_security_role_id` BIGINT NOT NULL,
    `method_type` VARCHAR(10) NOT NULL,
    `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `fk_auth_security_api_role_auth_security_api_idx` (`auth_security_api_id` ASC) VISIBLE,
    INDEX `fk_auth_security_api_role_auth_security_role1_idx` (`auth_security_role_id` ASC) VISIBLE,
    CONSTRAINT `fk_auth_security_api_role_auth_security_api`
    FOREIGN KEY (`auth_security_api_id`)
    REFERENCES `auth_security_api` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_auth_security_api_role_auth_security_role1`
    FOREIGN KEY (`auth_security_role_id`)
    REFERENCES `auth_security_role` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `hktv_ars`.`delivery_zone`
-- -----------------------------------------------------
CREATE TABLE `delivery_zone` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `zone_code` VARCHAR(50) DEFAULT NULL,
  `zone_desc` VARCHAR(200) DEFAULT NULL,
  `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `delivery_zone_idx1` (`zone_code`) VISIBLE
) ENGINE = InnoDB
      DEFAULT CHARACTER SET = utf8mb4
      COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `hktv_ars`.`district`
-- -----------------------------------------------------
CREATE TABLE `district` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `delivery_zone_code` VARCHAR(50) DEFAULT NULL,
  `district_code` VARCHAR(50) DEFAULT NULL,
  `district_name_en` VARCHAR(200) DEFAULT NULL,
  `district_name_zh` VARCHAR(200) DEFAULT NULL,
  `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `district_idx1` (`district_code`) VISIBLE
) ENGINE = InnoDB
      DEFAULT CHARACTER SET = utf8mb4
      COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `hktv_ars`.`estate`
-- -----------------------------------------------------
CREATE TABLE `estate` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `delivery_zone_code` VARCHAR(50) DEFAULT NULL,
  `district_code` VARCHAR(50) DEFAULT NULL,
  `estate_code` VARCHAR(50) DEFAULT NULL,
  `estate_name_en` VARCHAR(200) DEFAULT NULL,
  `estate_name_zh` VARCHAR(200) DEFAULT NULL,
  `is_active` TINYINT(1) DEFAULT NULL,
  `will_delivery` TINYINT(1) DEFAULT NULL,
  `latitude` DECIMAL(9,6) DEFAULT NULL,
  `longitude` DECIMAL(9,6) DEFAULT NULL,
  `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `estate_idx1` (`estate_code`) VISIBLE
) ENGINE = InnoDB
      DEFAULT CHARACTER SET = utf8mb4
      COLLATE = utf8mb4_unicode_ci;



-- -----------------------------------------------------
-- Table `hktv_ars`.`street`
-- -----------------------------------------------------
CREATE TABLE `street` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `delivery_zone_code` VARCHAR(50) DEFAULT NULL,
  `district_code` VARCHAR(50) DEFAULT NULL,
  `is_active` TINYINT(1) DEFAULT NULL,
  `will_delivery` TINYINT(1) DEFAULT NULL,
  `latitude` DECIMAL(9,6) DEFAULT NULL,
  `longitude` DECIMAL(9,6) DEFAULT NULL,
  `street_code` VARCHAR(50) DEFAULT NULL,
  `street_name_en` VARCHAR(200) DEFAULT NULL,
  `street_name_zh` VARCHAR(200) DEFAULT NULL,
  `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `street_idx1` (`street_code`) VISIBLE
) ENGINE = InnoDB
      DEFAULT CHARACTER SET = utf8mb4
      COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `hktv_ars`.`street_number`
-- -----------------------------------------------------
CREATE TABLE `street_number` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `delivery_zone_code` VARCHAR(50) DEFAULT NULL,
  `latitude` DECIMAL(9,6) DEFAULT NULL,
  `longitude` DECIMAL(9,6) DEFAULT NULL,
  `street_code` VARCHAR(50) DEFAULT NULL,
  `street_number_code` VARCHAR(50) DEFAULT NULL,
  `street_number` VARCHAR(10) DEFAULT NULL,
  `will_delivery` TINYINT(1) DEFAULT NULL,
  `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `street_number_idx1` (`street_number_code`) VISIBLE
) ENGINE = InnoDB
      DEFAULT CHARACTER SET = utf8mb4
      COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `hktv_ars`.`address_record`
-- -----------------------------------------------------

CREATE TABLE `address_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `address` varchar(255) DEFAULT NULL,
    `analysis_type` varchar(20) DEFAULT NULL,
    `status` varchar(50) DEFAULT NULL,
    `district` varchar(255) DEFAULT NULL,
    `estate` varchar(255) DEFAULT NULL,
    `street` varchar(255) DEFAULT NULL,
    `street_number` varchar(255) DEFAULT NULL,
    `original_delivery_zone_code` varchar(50) DEFAULT NULL,
    `actual_delivery_zone_code` varchar(50) DEFAULT NULL,
    `latitude` DECIMAL(9,6) DEFAULT NULL,
    `longitude` DECIMAL(9,6) DEFAULT NULL,
    `receive_time` DATETIME DEFAULT NULL,
    `update_by` bigint DEFAULT NULL,
    `will_delivery` TINYINT(1) DEFAULT NULL,
    `creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
       DEFAULT CHARACTER SET = utf8mb4
       COLLATE = utf8mb4_unicode_ci;
