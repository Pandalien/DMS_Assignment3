-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema carpooler
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema carpooler
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `carpooler` DEFAULT CHARACTER SET utf8 ;
USE `carpooler` ;

-- -----------------------------------------------------
-- Table `carpooler`.`user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `carpooler`.`user` (
  `user_id` INT NOT NULL AUTO_INCREMENT,
  `lat` DOUBLE NULL,
  `long` DOUBLE NULL,
  `driver_flag` INT NULL,
  `pionts` INT NULL,
  `proximity_range` DOUBLE NULL,
  `dest_lat` DOUBLE NULL,
  `dest_long` DOUBLE NULL,
  `name` VARCHAR(100) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`user_id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `carpooler`.`transaction`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `carpooler`.`transaction` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `driver_id` INT NOT NULL,
  `passenger_id` INT NOT NULL,
  `datetime` DATETIME NULL,
  `action` INT NOT NULL,
  `lat` DOUBLE NULL,
  `long` DOUBLE NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_transaction_user_idx` (`driver_id` ASC),
  INDEX `fk_transaction_user1_idx` (`passenger_id` ASC),
  CONSTRAINT `fk_transaction_user`
    FOREIGN KEY (`driver_id`)
    REFERENCES `carpooler`.`user` (`user_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_transaction_user1`
    FOREIGN KEY (`passenger_id`)
    REFERENCES `carpooler`.`user` (`user_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
