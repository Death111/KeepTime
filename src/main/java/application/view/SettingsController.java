package application.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.controller.Controller;
import application.model.Model;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
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
   private Button saveButton;

   @FXML
   private Button cancelButton;

   private final Logger Log = LoggerFactory.getLogger(this.getClass());

   private Model model;
   private Controller controller;
   private Stage thisStage;

   @FXML
   private void initialize() {

      saveButton.setOnAction(ae -> {
         Log.info("Save clicked");
         controller.updateSettings(hoverBackgroundColor.getValue(), hoverFontColor.getValue(),
               defaultBackgroundColor.getValue(), defaultFontColor.getValue(), taskBarColor.getValue(),
               useHotkeyCheckBox.isSelected());
         thisStage.close();
      });

      cancelButton.setOnAction(ae -> {
         Log.info("Cancel clicked");
         thisStage.close();
      });

      resetHoverBackgroundButton.setOnAction(ae -> {
         hoverBackgroundColor.setValue(Model.originalHoverBackgroundColor);
      });
      resetHoverFontButton.setOnAction(ae -> {
         hoverFontColor.setValue(Model.originalHoverFontColor);
      });
      resetDefaultBackgroundButton.setOnAction(ae -> {
         defaultBackgroundColor.setValue(Model.originalDefaultBackgroundColor);
      });
      resetDefaultFontButton.setOnAction(ae -> {
         defaultFontColor.setValue(Model.originalDefaultFontColor);
      });
      resetTaskBarFontButton.setOnAction(ae -> {
         taskBarColor.setValue(Model.originalTaskBarFontColor);
      });

   }

   public void setModelAndController(final Model model, final Controller controller) {
      this.model = model;
      this.controller = controller;

      update();
   }

   void update() {
      hoverBackgroundColor.setValue(model.hoverBackgroundColor.get());
      hoverFontColor.setValue(model.hoverFontColor.get());

      defaultBackgroundColor.setValue(model.defaultBackgroundColor.get());
      defaultFontColor.setValue(model.defaultFontColor.get());

      taskBarColor.setValue(model.taskBarColor.get());

      useHotkeyCheckBox.setSelected(model.useHotkey.get());
   }

   public void setStage(final Stage thisStage) {
      this.thisStage = thisStage;
   }
}
