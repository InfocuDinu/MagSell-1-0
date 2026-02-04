package com.magsell.ui.controllers;

import javafx.fxml.FXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller simplu pentru modulele în dezvoltare
 */
public class SimpleViewController {
    private static final Logger logger = LoggerFactory.getLogger(SimpleViewController.class);
    
    @FXML
    public void initialize() {
        logger.info("Initializing SimpleViewController");
    }
}
