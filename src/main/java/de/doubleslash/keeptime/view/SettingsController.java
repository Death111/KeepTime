package de.doubleslash.keeptime.view;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.doubleslash.keeptime.Main;
import de.doubleslash.keeptime.common.ConfigParser;
import de.doubleslash.keeptime.controller.Controller;
import de.doubleslash.keeptime.model.Model;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class SettingsController {
   @FXML
   private ColorPicker hoverBackgroundColor;
   @FXML
   private ColorPicker hoverFontColor;

   @FXML
   private ColorPicker defaultBackgroundColor;
   @FXML
   private ColorPicker defaultFontColor;

   @FXML
   private ColorPicker taskBarColor;

   @FXML
   private Button resetHoverBackgroundButton;
   @FXML
   private Button resetHoverFontButton;
   @FXML
   private Button resetDefaultBackgroundButton;
   @FXML
   private Button resetDefaultFontButton;
   @FXML
   private Button resetTaskBarFontButton;

   @FXML
   private CheckBox useHotkeyCheckBox;
   @FXML
   private CheckBox displayProjectsRightCheckBox;
   @FXML
   private CheckBox hideProjectsOnMouseExitCheckBox;

   @FXML
   private Button saveButton;

   @FXML
   private Button cancelButton;

   @FXML
   private Button parseConfigButton;

   @FXML
   private Label versionLabel;

   @FXML
   private Label useHotkeyLabel;
   @FXML
   private Label hotkeyLabel;
   @FXML
   private Label globalKeyloggerLabel;

   private static final Logger LOG = LoggerFactory.getLogger(SettingsController.class);

   private Controller controller;
   private Stage thisStage;
   private static final String INPUT_FILE = "config.xml";

   @FXML
   private void initialize() {
	  LOG.debug("start init");
	  LOG.info("OS: {}", System.getProperty("os.name"));
      LOG.debug("set versionLabel text");
      versionLabel.setText(Main.VERSION);
      
      if(System.getProperty("os.name").contains("Linux")) {
    	  if(useHotkeyCheckBox != null) {
    		  useHotkeyCheckBox.setDisable(true);
    	  } else {
    		  LOG.warn("useHotkeyCheckBox is null");
    	  }
    	  
    	  if(useHotkeyLabel != null) {
    		  useHotkeyLabel.setDisable(true);
    	  } else {
    		  LOG.warn("useHotkeyLabel is null");
    	  }
    	  
    	  if(hotkeyLabel != null) {
    		  hotkeyLabel.setDisable(true);
    	  } else {
    		  LOG.warn("hotkeyLabel is null");
    	  }
    	  
    	  if(globalKeyloggerLabel != null) {
    		  globalKeyloggerLabel.setDisable(true);
    	  } else {
    		  LOG.warn("globalKeyloggerLabel is null");
    	  }
      }
      
      LOG.debug("saveButton.setOnAction");
      saveButton.setOnAction(ae -> {
         LOG.info("Save clicked");

         if (System.getProperty("os.name").contains("Linux")) {
            if (hoverBackgroundColor.getValue().getOpacity() < 0.5) {
               hoverBackgroundColor.setValue(Color.rgb((int) (hoverBackgroundColor.getValue().getRed() * 255),
                     (int) (hoverBackgroundColor.getValue().getGreen() * 255),
                     (int) (hoverBackgroundColor.getValue().getBlue() * 255), 0.51));
               final Alert alert = new Alert(AlertType.WARNING);
               alert.setTitle("Warning!");
               alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
               alert.setHeaderText("color settings wrong!");
               alert.setContentText("The level of opacity on your hover background is to high for Linux.");

               alert.showAndWait();
            }
            if (defaultBackgroundColor.getValue().getOpacity() < 0.5) {
               defaultBackgroundColor.setValue(Color.rgb((int) (defaultBackgroundColor.getValue().getRed() * 255),
                     (int) (defaultBackgroundColor.getValue().getGreen() * 255),
                     (int) (defaultBackgroundColor.getValue().getBlue() * 255), 0.51));
               final Alert alert = new Alert(AlertType.WARNING);
               alert.setTitle("Warning!");
               alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
               alert.setHeaderText("color settings wrong!");
               alert.setContentText("The level of opacity on your default background is to high for Linux.");

               alert.showAndWait();
            }
         }

         if (!displayProjectsRightCheckBox.isSelected()) {
            final Alert warning = new Alert(AlertType.WARNING);
            warning.setTitle("No Linux Support");
            warning.setHeaderText(null);
            warning.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            warning.setContentText(
                  "The project list on the left side has no Linux support and doesn't run quite well on Windows ether. Please change your settings so, that your project list is on the right side.");
            warning.showAndWait();
         }

         controller.updateSettings(hoverBackgroundColor.getValue(), hoverFontColor.getValue(),
               defaultBackgroundColor.getValue(), defaultFontColor.getValue(), taskBarColor.getValue(),
               useHotkeyCheckBox.isSelected(), displayProjectsRightCheckBox.isSelected(),
               hideProjectsOnMouseExitCheckBox.isSelected());
         thisStage.close();
      });

      LOG.debug("cancelButton.setOnAction");
      cancelButton.setOnAction(ae -> {
         LOG.info("Cancel clicked");
         thisStage.close();
      });

      LOG.debug("reset*Button.setOnAction");
      resetHoverBackgroundButton
            .setOnAction(ae -> hoverBackgroundColor.setValue(Model.ORIGINAL_HOVER_BACKGROUND_COLOR));
      resetHoverFontButton.setOnAction(ae -> hoverFontColor.setValue(Model.ORIGINAL_HOVER_Font_COLOR));
      resetDefaultBackgroundButton
            .setOnAction(ae -> defaultBackgroundColor.setValue(Model.ORIGINAL_DEFAULT_BACKGROUND_COLOR));
      resetDefaultFontButton.setOnAction(ae -> defaultFontColor.setValue(Model.ORIGINAL_DEFAULT_FONT_COLOR));
      resetTaskBarFontButton.setOnAction(ae -> taskBarColor.setValue(Model.ORIGINAL_TASK_BAR_FONT_COLOR));

      LOG.debug("parseConfigButton.setOnAction");
      parseConfigButton.setOnAction(actionEvent -> {
         if (ConfigParser.hasConfigFile(INPUT_FILE)) {
            final ConfigParser parser = new ConfigParser(controller);
            parser.parseConfig(new File(INPUT_FILE));
         }
      });

   }

   public void setController(final Controller controller) {
      this.controller = controller;

      update();
   }

   void update() {
      hoverBackgroundColor.setValue(Model.HOVER_BACKGROUND_COLOR.get());
      hoverFontColor.setValue(Model.HOVER_FONT_COLOR.get());

      defaultBackgroundColor.setValue(Model.DEFAULT_BACKGROUND_COLOR.get());
      defaultFontColor.setValue(Model.DEFAULT_FONT_COLOR.get());

      taskBarColor.setValue(Model.TASK_BAR_COLOR.get());

      useHotkeyCheckBox.setSelected(Model.USE_HOTKEY.get());
      displayProjectsRightCheckBox.setSelected(Model.DISPLAY_PROJECTS_RIGHT.get());
      hideProjectsOnMouseExitCheckBox.setSelected(Model.HIDE_PROJECTS_ON_MOUSE_EXIT.get());
   }

   public void setStage(final Stage thisStage) {
      this.thisStage = thisStage;
   }
}
