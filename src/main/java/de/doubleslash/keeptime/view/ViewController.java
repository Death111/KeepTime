// Copyright 2019 doubleSlash Net Business GmbH
//
// This file is part of KeepTime.
// KeepTime is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

package de.doubleslash.keeptime.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.doubleslash.keeptime.common.ColorHelper;
import de.doubleslash.keeptime.common.DateFormatter;
import de.doubleslash.keeptime.common.Resources;
import de.doubleslash.keeptime.common.Resources.RESOURCE;
import de.doubleslash.keeptime.common.ScreenPosHelper;
import de.doubleslash.keeptime.common.StyleUtils;
import de.doubleslash.keeptime.common.time.Interval;
import de.doubleslash.keeptime.controller.Controller;
import de.doubleslash.keeptime.exceptions.FXMLLoaderException;
import de.doubleslash.keeptime.model.Model;
import de.doubleslash.keeptime.model.Project;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Component
public class ViewController {
   private static final Logger LOG = LoggerFactory.getLogger(ViewController.class);
   private static final String TIME_ZERO = "00:00:00";

   @FXML
   private Pane pane;
   @FXML
   private BorderPane borderPane;

   @FXML
   private ListView<Project> availableProjectsListView;

   @FXML
   private VBox projectsVBox;

   @FXML
   private Label bigTimeLabel;
   @FXML
   private Label allTimeLabel;
   @FXML
   private Label todayAllSeconds;

   @FXML
   private Label currentProjectLabel;

   @FXML
   private Button minimizeButton;
   @FXML
   private Button closeButton;

   @FXML
   private Button addNewProjectButton;

   @FXML
   private TextField searchTextField;

   @FXML
   private Button settingsButton;
   @FXML
   private Button calendarButton;

   @FXML
   private TextArea textArea;

   @FXML
   private Canvas canvas;

   @FXML
   private FontAwesomeIconView calendarIcon;

   @FXML
   private FontAwesomeIconView settingsIcon;

   @FXML
   private FontAwesomeIconView minimizeIcon;

   @FXML
   private FontAwesomeIconView closeIcon;

   private ColorTimeLine mainColorTimeLine;

   private class Delta {
      double x;
      double y;
   }

   private final Delta dragDelta = new Delta();

   private Stage mainStage;

   private final Controller controller;

   private final Model model;

   private final Canvas taskbarCanvas = new Canvas(32, 32);

   private final BooleanProperty mouseHoveringProperty = new SimpleBooleanProperty(false);
   public static final LongProperty activeWorkSecondsProperty = new SimpleLongProperty(0);
   public static final ObjectProperty<Color> fontColorProperty = new SimpleObjectProperty<>();

   private Stage reportStage;
   private ReportController reportController;

   private Stage settingsStage;
   private SettingsController settingsController;

   private ProjectsListViewController projectsListViewController;

   @Autowired
   public ViewController(final Model model, final Controller controller) {
      this.model = model;
      this.controller = controller;
   }

   @FXML
   private void initialize() {

      availableProjectsListView.setFixedCellSize(13);

      setUpTime();

      setUpTextArea();

      // reposition window if projects are hidden (as anchor is top left)
      mouseHoveringProperty.addListener((a, b, c) -> {
         if (!model.hideProjectsOnMouseExit.get()) {
            setProjectListVisible(true);
            return;
         }

         setProjectListVisible(c);
      });

      minimizeButton.setOnAction(ae -> mainStage.setIconified(true));
      minimizeButton.textFillProperty().bind(fontColorProperty);
      closeButton.setOnAction(ae -> mainStage.close());
      closeButton.textFillProperty().bind(fontColorProperty);

      addNewProjectButton.textFillProperty().bind(fontColorProperty);

      settingsButton.setOnAction(ae -> settingsClicked());

      calendarButton.setOnAction(ae -> calendarClicked());

      calendarIcon.fillProperty().bind(fontColorProperty);
      settingsIcon.fillProperty().bind(fontColorProperty);
      minimizeIcon.fillProperty().bind(fontColorProperty);
      closeIcon.fillProperty().bind(fontColorProperty);

      final Runnable updateMainBackgroundColor = this::runUpdateMainBackgroundColor;

      mouseHoveringProperty.addListener((a, b, c) -> updateMainBackgroundColor.run());

      Platform.runLater(() -> {
         loadSubStages();
         fontColorProperty.set(model.defaultFontColor.get());
         fontColorProperty.bind(Bindings.createObjectBinding(() -> {
            if (mouseHoveringProperty.get()) {
               return model.hoverFontColor.get();
            } else {
               return model.defaultFontColor.get();
            }
         }, mouseHoveringProperty, model.defaultFontColor, model.hoverFontColor));

         bigTimeLabel.textFillProperty().bind(fontColorProperty);
         allTimeLabel.textFillProperty().bind(fontColorProperty);
         todayAllSeconds.textFillProperty().bind(fontColorProperty);
         currentProjectLabel.textFillProperty().bind(fontColorProperty);

         textArea.setText("");
         textArea.requestFocus();

         final Runnable displayProjectRightRunnable = () -> {
            if (model.displayProjectsRight.get()) {
               borderPane.setLeft(null);
               borderPane.setRight(projectsVBox);
            } else {
               borderPane.setRight(null);
               borderPane.setLeft(projectsVBox);
            }
         };
         model.displayProjectsRight.addListener((a, oldValue, newValue) -> displayProjectRightRunnable.run());
         displayProjectRightRunnable.run();

         // Setup textarea font color binding
         final Runnable textAreaColorRunnable = () -> {
            final String textAreaStyle = StyleUtils.changeStyleAttribute(textArea.getStyle(), "fx-text-fill",
                  "rgba(" + ColorHelper.colorToCssRgba(fontColorProperty.get()) + ")");
            textArea.setStyle(textAreaStyle);
         };
         fontColorProperty.addListener((a, b, c) -> textAreaColorRunnable.run());
         textAreaColorRunnable.run();

         model.activeWorkItem.addListener((a, b, c) -> {
            updateProjectView();
            textArea.setText("");
            textArea.requestFocus();
         });

         model.defaultBackgroundColor.addListener((a, b, c) -> updateMainBackgroundColor.run());
         model.hoverBackgroundColor.addListener((a, b, c) -> updateMainBackgroundColor.run());
         updateMainBackgroundColor.run();
      });

      pane.setOnMouseEntered(a -> mouseHoveringProperty.set(true));

      pane.setOnMouseExited(a -> mouseHoveringProperty.set(false));

      // Drag stage
      pane.setOnMousePressed(mouseEvent -> {

         // record a delta distance for the drag and drop operation.
         dragDelta.x = mainStage.getX() - mouseEvent.getScreenX();
         dragDelta.y = mainStage.getY() - mouseEvent.getScreenY();
      });

      pane.setOnMouseDragged(mouseEvent -> {
         mainStage.setX(mouseEvent.getScreenX() + dragDelta.x);
         mainStage.setY(mouseEvent.getScreenY() + dragDelta.y);
      });

      bigTimeLabel.textProperty().bind(Bindings.createStringBinding(
            () -> DateFormatter.secondsToHHMMSS(activeWorkSecondsProperty.get()), activeWorkSecondsProperty));

      // update ui each second
      new Interval(1).registerCallBack(() -> {
         final LocalDateTime now = LocalDateTime.now();
         model.activeWorkItem.get().setEndTime(now); // FIXME not good to change model

         final long currentWorkSeconds = DateFormatter.getSecondsBewtween(model.activeWorkItem.get().getStartTime(), model.activeWorkItem.get().getEndTime());
         activeWorkSecondsProperty.set(currentWorkSeconds);
         final long todayWorkingSeconds = controller.calcTodaysWorkSeconds();
         final long todaySeconds = controller.calcTodaysSeconds();

         // update all ui labels
         allTimeLabel.setText(DateFormatter.secondsToHHMMSS(todayWorkingSeconds));
         todayAllSeconds.setText(DateFormatter.secondsToHHMMSS(todaySeconds));

         projectsListViewController.tick();

         mainColorTimeLine.update(model.getSortedPastWorkItems(), controller.calcTodaysSeconds());
         updateTaskbarIcon(currentWorkSeconds);

      });

      mainColorTimeLine = new ColorTimeLine(canvas);

      controller.changeProject(model.getIdleProject(), 0);

      updateProjectView();

   }

   private Dialog<Project> dialogResultConverter(final Dialog<Project> dialog,
         final ManageProjectController manageProjectController) {
      dialog.setResultConverter(dialogButton -> {
         if (dialogButton == ButtonType.OK) {
            return manageProjectController.getProjectFromUserInput();
         }
         return null;
      });
      return dialog;
   }

   private void settingsClicked() {
      LOG.info("Settings clicked");
      this.mainStage.setAlwaysOnTop(false);
      settingsController.update();
      settingsStage.show();
   }

   private void calendarClicked() {
      LOG.info("Calendar clicked");
      this.mainStage.setAlwaysOnTop(false);
      reportStage.setAlwaysOnTop(true);
      reportController.update();
      reportStage.show();
   }

   private void runUpdateMainBackgroundColor() {
      Color color = model.defaultBackgroundColor.get();
      double opacity = 0;
      if (mouseHoveringProperty.get()) {
         color = model.hoverBackgroundColor.get();
         opacity = .3;
      }
      String style = StyleUtils.changeStyleAttribute(pane.getStyle(), "fx-background-color",
            "rgba(" + ColorHelper.colorToCssRgba(color) + ")");
      style = StyleUtils.changeStyleAttribute(style, "fx-border-color",
            "rgba(" + ColorHelper.colorToCssRgb(color) + ", " + opacity + ")");
      pane.setStyle(style);
   }

   private void setUpTime() {
      bigTimeLabel.setText(TIME_ZERO);
      allTimeLabel.setText(TIME_ZERO);
      todayAllSeconds.setText(TIME_ZERO);
   }

   private void setUpTextArea() {
      textArea.setWrapText(true);
      textArea.setEditable(false);
      textArea.editableProperty().bind(mouseHoveringProperty);

      textArea.textProperty().addListener((a, b, c) -> controller.setComment(textArea.getText()));
   }

   private void setProjectListVisible(final boolean showProjectList) {
      projectsVBox.setManaged(showProjectList);
      final double beforeWidth = mainStage.getWidth();
      mainStage.sizeToScene();
      final double afterWidth = mainStage.getWidth();
      projectsVBox.setVisible(showProjectList);
      final double offset = afterWidth - beforeWidth;
      if (!model.displayProjectsRight.get()) {
         // we only need to move the stage if the node on the left is hidden
         // not sure how we can prevent the jumping
         mainStage.setX(mainStage.getX() - offset);
      }
   }

   private void loadSubStages() {
      try {
         // Report stage
         final FXMLLoader fxmlLoader = createFXMLLoader(RESOURCE.FXML_REPORT);
         fxmlLoader.setControllerFactory(model.getSpringContext()::getBean);
         final Parent root = fxmlLoader.load();
         root.setFocusTraversable(true);
         root.requestFocus();
         reportController = fxmlLoader.getController();
         reportStage = new Stage();
         reportStage.initModality(Modality.APPLICATION_MODAL);
         reportController.setStage(reportStage);
         final Scene reportScene = new Scene(root);
         reportScene.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ESCAPE) {
               LOG.info("pressed ESCAPE");
               reportStage.close();
            }
         });

         reportStage.setScene(reportScene);
         reportStage.setTitle("Report");
         reportStage.setResizable(false);
         reportStage.setOnHiding(windowEvent -> {
            reportStage.setAlwaysOnTop(false);
            this.mainStage.setAlwaysOnTop(true);
         });

         // Settings stage
         final FXMLLoader fxmlLoader2 = createFXMLLoader(RESOURCE.FXML_SETTINGS);
         fxmlLoader2.setControllerFactory(model.getSpringContext()::getBean);
         final Parent settingsRoot = fxmlLoader2.load();
         settingsController = fxmlLoader2.getController();
         settingsStage = new Stage();
         settingsController.setStage(settingsStage);
         settingsStage.initModality(Modality.APPLICATION_MODAL);
         settingsStage.setTitle("Settings");
         settingsStage.setResizable(false);

         final Scene settingsScene = new Scene(settingsRoot);
         settingsScene.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ESCAPE) {
               LOG.info("pressed ESCAPE");
               settingsStage.close();
            }
         });

         settingsStage.setScene(settingsScene);
         settingsStage.setOnHiding(e -> this.mainStage.setAlwaysOnTop(true));
      } catch (final IOException e) {
         LOG.error("Error while loading sub stage");
         throw new FXMLLoaderException(e);
      }
   }

   private FXMLLoader createFXMLLoader(final RESOURCE fxmlLayout) {
      return new FXMLLoader(Resources.getResource(fxmlLayout));
   }

   private Dialog<Project> setUpDialogProject(final String title, final String headerText) {
      final Dialog<Project> dialog = new Dialog<>();
      dialog.setTitle(title);
      dialog.setHeaderText(headerText);
      dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
      setUpAddNewProjectGridPane(dialog);

      // TODO disable OK button if no name is set
      return dialog;
   }

   private GridPane setUpAddNewProjectGridPane(final Dialog<Project> dialog) {
      GridPane grid;
      final FXMLLoader loader = new FXMLLoader(Resources.getResource(RESOURCE.FXML_MANAGE_PROJECT));
      loader.setControllerFactory(model.getSpringContext()::getBean);
      try {
         grid = loader.load();
      } catch (final IOException e) {
         throw new FXMLLoaderException(String.format("Error while loading '%s'.", RESOURCE.FXML_MANAGE_PROJECT), e);
      }

      dialog.getDialogPane().setContent(grid);

      final ManageProjectController manageProjectController = loader.getController();

      dialogResultConverter(dialog, manageProjectController);

      return grid;
   }

   private void updateTaskbarIcon(final long currentWorkSeconds) {
      final GraphicsContext gcIcon = taskbarCanvas.getGraphicsContext2D();

      gcIcon.clearRect(0, 0, taskbarCanvas.getWidth(), taskbarCanvas.getHeight());
      gcIcon.setFill(model.activeWorkItem.get().getProject().getColor());
      gcIcon.fillRect(1, 27, 31, 5);

      gcIcon.setStroke(model.taskBarColor.get());
      gcIcon.setTextAlign(TextAlignment.CENTER);
      gcIcon.strokeText(DateFormatter.secondsToHHMMSS(currentWorkSeconds).replaceFirst(":", ":\n"),
            Math.round(taskbarCanvas.getWidth() / 2), Math.round(taskbarCanvas.getHeight() / 2) - 5.0);

      final SnapshotParameters snapshotParameters = new SnapshotParameters();
      snapshotParameters.setFill(Color.TRANSPARENT);
      final WritableImage image = taskbarCanvas.snapshot(snapshotParameters, null);

      final StackPane layout = new StackPane();
      layout.getChildren().addAll(new ImageView(image));

      final BufferedImage bi = SwingFXUtils.fromFXImage(image, null);
      final Image icon = SwingFXUtils.toFXImage(bi, null);

      final ObservableList<Image> icons = mainStage.getIcons();
      icons.addAll(icon);
      if (icons.size() > 1) {
         icons.remove(0);
      }
   }

   private void updateProjectView() {
      final Project project = model.activeWorkItem.get().getProject();
      currentProjectLabel.setText(project.getName());
      currentProjectLabel.setUnderline(project.isWork());
      final Circle circle = new Circle(4);
      circle.setFill(project.getColor());
      currentProjectLabel.setGraphic(circle);
   }

   public void setStage(final Stage primaryStage) {
      this.mainStage = primaryStage;
      this.projectsListViewController = new ProjectsListViewController(model, controller, mainStage,
            availableProjectsListView, searchTextField, false);
      setupStagePositioning();
   }

   @FXML
   public void addNewProject(final ActionEvent ae) {
      LOG.info("Add new project clicked");
      // TODO somewhat duplicate dialog of create and edit
      final Dialog<Project> dialog = setUpDialogProject("Create new project", "Create a new project");

      mainStage.setAlwaysOnTop(false);
      final Optional<Project> result = dialog.showAndWait();
      mainStage.setAlwaysOnTop(true);

      result.ifPresent(project -> controller.addNewProject(project));
   }

   private void setupStagePositioning() {
      final ScreenPosHelper positionHelper = new ScreenPosHelper(model.screenSettings.screenHash.get(),
            model.screenSettings.proportionalX.get(), model.screenSettings.proportionalY.get());
      positionHelper.resetPositionIfInvalid();

      // set stage to saved Position
      if (model.screenSettings.saveWindowPosition.get()) {
         mainStage.setX(positionHelper.getAbsoluteX());
         mainStage.setY(positionHelper.getAbsoluteY());
      }

      // add listeners to record Windowpositionchange
      final ChangeListener<Number> positionChangeListener = (final ObservableValue<? extends Number> observable,
            final Number oldValue, final Number newValue) -> {
         savePosition();
      };

      mainStage.xProperty().addListener(positionChangeListener);
      mainStage.yProperty().addListener(positionChangeListener);
   }

   public void savePosition() {
      // don't save if option disabled
      if (!model.screenSettings.saveWindowPosition.get()) {
         return;
      }

      LOG.debug("Stage position changed '{}'/'{}'.", mainStage.xProperty().doubleValue(),
            mainStage.yProperty().doubleValue());

      final ScreenPosHelper positionHelper = new ScreenPosHelper(mainStage.xProperty().doubleValue(),
            mainStage.yProperty().doubleValue());
      model.screenSettings.screenHash.set(positionHelper.getScreenHash());
      model.screenSettings.proportionalX.set(positionHelper.getProportionalX());
      model.screenSettings.proportionalY.set(positionHelper.getProportionalY());

   }

}
