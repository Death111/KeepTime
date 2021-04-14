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

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.doubleslash.keeptime.common.DateFormatter;
import de.doubleslash.keeptime.common.Resources;
import de.doubleslash.keeptime.common.Resources.RESOURCE;
import de.doubleslash.keeptime.controller.Controller;
import de.doubleslash.keeptime.exceptions.FXMLLoaderException;
import de.doubleslash.keeptime.model.Model;
import de.doubleslash.keeptime.model.Project;
import de.doubleslash.keeptime.model.Work;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Bloom;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ProjectsListViewController {

   private static final Logger LOG = LoggerFactory.getLogger(ProjectsListViewController.class);

   private final Model model;
   private final Controller controller;
   private final Stage mainStage;
   private final Map<Project, Label> elapsedProjectTimeLabelMap = new HashMap<>();
   private final Map<Project, Node> projectSelectionNodeMap;
   private final FilteredList<Project> filteredData;

   private final boolean hideable;

   public ProjectsListViewController(final Model model, final Controller controller, final Stage mainStage,
         final ListView<Project> availableProjectsListView, final TextField searchTextField, final boolean hideable) {
      this.model = model;
      this.controller = controller;
      this.hideable = hideable;
      this.mainStage = mainStage;
      availableProjectsListView.setCellFactory(listView -> returnListCellOfProject());

      filteredData = new FilteredList<>(model.getSortedAvailableProjects(), p -> true);
      availableProjectsListView.setItems(filteredData);

      projectSelectionNodeMap = new HashMap<>(model.getAvailableProjects().size());

      searchTextField.textProperty().addListener((a, b, newValue) -> {
         LOG.info("New filter value: " + newValue);
         // TODO do i always have to create a new predicate?
         filteredData.setPredicate(project -> doesProjectMatchSearchFilter(newValue, project));
         LOG.debug("Amount of projects to show '{}'.", filteredData.size());
         availableProjectsListView.getSelectionModel().selectFirst();
         availableProjectsListView.scrollTo(0);
      });

      searchTextField.setOnKeyPressed(eh -> {
         final MultipleSelectionModel<Project> selectionModel = availableProjectsListView.getSelectionModel();
         switch (eh.getCode()) {
         case UP:
            LOG.debug("Arrow up pressed.");
            selectionModel.selectPrevious();
            eh.consume();
            break;
         case DOWN:
            LOG.debug("Arrow down pressed.");
            selectionModel.selectNext();
            eh.consume();
            break;
         case ESCAPE:
            LOG.debug("Esc pressed.");
            if (hideable) {
               mainStage.hide();
            }
            eh.consume();
            break;
         default:
            break;
         }
         availableProjectsListView.scrollTo(selectionModel.getSelectedIndex());
      });

      // enter pressed in textfield
      searchTextField.setOnAction(ev -> {
         final Project selectedItem = availableProjectsListView.getSelectionModel().getSelectedItem();
         if (selectedItem != null) {
            changeProject(selectedItem, 0);
            searchTextField.setText("");
         }
      });

      availableProjectsListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
      availableProjectsListView.getSelectionModel().selectFirst();
   }

   static boolean doesProjectMatchSearchFilter(final String searchText, final Project project) {
      // If filter text is empty, display all data.
      if (searchText == null || searchText.isEmpty()) {
         return true;
      }

      final String lowerCaseFilter = searchText.toLowerCase();

      if (project.getName().toLowerCase().contains(lowerCaseFilter)
            || project.getDescription().toLowerCase().contains(lowerCaseFilter)) {
         return true;
      }

      return false;
   }

   /**
    * Update UI
    */
   public void tick() {
      for (final Entry<Project, Label> entry : elapsedProjectTimeLabelMap.entrySet()) {
         final Project p = entry.getKey();
         final Label label = entry.getValue();

         final long seconds = model.getPastWorkItems().stream().filter(work -> work.getProject().getId() == p.getId())
               .mapToLong(work -> DateFormatter.getSecondsBewtween(work.getStartTime(), work.getEndTime())).sum();
         label.setText(DateFormatter.secondsToHHMMSS(seconds));
      }
   }

   private void changeProject(final Project newProject, final long minusSeconds) {
      if (hideable) {
         mainStage.hide();
      }
      if (model.remindIfNotesAreEmpty.get()) {
         final Work currentWork = model.activeWorkItem.get();
         if (currentWork != null && currentWork.getNotes().isEmpty()) {
            final TextInputDialog noteDialog = new TextInputDialog();
            noteDialog.setTitle("Empty Notes");
            noteDialog.setHeaderText("Switch projects without notes?");
            noteDialog.setContentText(
                  "What did you do for project '" + model.activeWorkItem.get().getProject().getName() + "' ?");
            noteDialog.initOwner(mainStage);

            final Optional<String> result = noteDialog.showAndWait();
            if (result.isPresent()) {
               currentWork.setNotes(result.get());
            } else {
               // cancel pressed
               return;
            }
         }
      }
      controller.changeProject(newProject, minusSeconds);

   }

   private void addProjectToProjectSelectionNodeMap(final Project project) {
      final Node projectElement = createListEntryForProject(project);
      projectSelectionNodeMap.put(project, projectElement);
      updateTooltip(project);
   }

   private void realignProjectList() {
      LOG.debug("Sorting project view");
      // TODO changing the model is not ok from here, but the list is not resorted
      final Comparator<? super Project> comparator = model.getSortedAvailableProjects().getComparator();
      model.getSortedAvailableProjects().setComparator(null); // TODO is there a better way?
      model.getSortedAvailableProjects().setComparator(comparator);
   }

   private Node createListEntryForProject(final Project p) {
      final ContextMenu contextMenu = new ContextMenu();

      final FXMLLoader loader = new FXMLLoader();
      loader.setLocation(Resources.getResource(RESOURCE.FXML_PROJECT_LAYOUT));
      loader.setControllerFactory(model.getSpringContext()::getBean);
      Pane projectElement;
      try {
         projectElement = loader.load();
      } catch (final IOException e1) {
         LOG.error("Could not load '{}'.", loader.getLocation());
         throw new FXMLLoaderException(e1);
      }

      final Label projectNameLabel = (Label) projectElement.getChildren().get(0);
      final Label elapsedTimeLabel = (Label) projectElement.getChildren().get(1);
      elapsedTimeLabel.textFillProperty().bind(ViewController.fontColorProperty);
      elapsedProjectTimeLabelMap.put(p, elapsedTimeLabel);

      projectNameLabel.setText(p.getName());
      projectNameLabel.setUnderline(p.isWork());
      final Color color = p.getColor();
      final double dimFactor = .6;
      final Color dimmedColor = new Color(color.getRed() * dimFactor, color.getGreen() * dimFactor,
            color.getBlue() * dimFactor, 1);
      projectNameLabel.setTextFill(dimmedColor);

      projectNameLabel.setUserData(p);
      projectNameLabel.setOnContextMenuRequested(
            event -> contextMenu.show(projectNameLabel, event.getScreenX(), event.getScreenY()));

      projectNameLabel.setOnMouseClicked(a -> {

         final MouseButton button = a.getButton();
         if (button == MouseButton.PRIMARY) {
            changeProject(p, 0);
         }

      });
      projectNameLabel.setOnMouseEntered(ae -> {
         projectNameLabel.setTextFill(p.getColor());
         final Bloom bloom = new Bloom();
         bloom.setThreshold(0.3);
         projectNameLabel.setEffect(bloom);
      });

      projectNameLabel.setOnMouseExited(ae -> {
         projectNameLabel.setTextFill(new Color(p.getColor().getRed() * dimFactor, p.getColor().getGreen() * dimFactor,
               p.getColor().getBlue() * dimFactor, 1));
         projectNameLabel.setEffect(null);
      });

      final MenuItem changeWithTimeMenuItem = new MenuItem("Change with time");
      changeWithTimeMenuItem.setOnAction(e -> {
         final ChangeWithTimeDialog changeWithTimeDialog = new ChangeWithTimeDialog(model,
               ViewController.activeWorkSecondsProperty, p);
         mainStage.setAlwaysOnTop(false);
         final Optional<Integer> result = changeWithTimeDialog.showAndWait();
         result.ifPresent(minusSeconds -> changeProject(p, minusSeconds));
         mainStage.setAlwaysOnTop(true);
      });
      final MenuItem deleteMenuItem = new MenuItem("Delete");
      deleteMenuItem.setDisable(p.isDefault());
      deleteMenuItem.setOnAction(e -> {
         LOG.info("Delete");

         final Alert alert = new Alert(AlertType.CONFIRMATION);
         alert.setTitle("Delete project");
         alert.setHeaderText("Delete project '" + p.getName() + "'.");
         alert.setContentText(
               "The project will just be hidden from display, as there may be work references to this project.");

         mainStage.setAlwaysOnTop(false);
         final Optional<ButtonType> result = alert.showAndWait();
         mainStage.setAlwaysOnTop(true);

         result.ifPresent(res -> {
            if (result.get() == ButtonType.OK) {
               controller.deleteProject(p);
               realignProjectList();
            }
         });
      });

      final MenuItem editMenuItem = new MenuItem("Edit");
      editMenuItem.setOnAction(e -> {
         // TODO refactor to use "add project" controls
         LOG.info("Edit project");
         final Dialog<Project> dialog = setupEditProjectDialog("Edit project", "Edit project '" + p.getName() + "'", p);

         mainStage.setAlwaysOnTop(false);
         final Optional<Project> result = dialog.showAndWait();
         mainStage.setAlwaysOnTop(true);

         result.ifPresent(editedProject -> {
            controller.editProject(p, editedProject);

            projectNameLabel.setText(p.getName());
            projectNameLabel.setTextFill(new Color(p.getColor().getRed() * dimFactor,
                  p.getColor().getGreen() * dimFactor, p.getColor().getBlue() * dimFactor, 1));
            projectNameLabel.setUnderline(p.isWork());
            projectNameLabel.getTooltip().setText(p.getName());

            // TODO how to update currentProjectLabel when active project was edited?
            realignProjectList();
            updateTooltip(p);
         });
      });

      // Add MenuItem to ContextMenu
      contextMenu.getItems().addAll(changeWithTimeMenuItem, editMenuItem, deleteMenuItem);

      return projectElement;
   }

   private void updateTooltip(final Project p) {
      final Pane pane = (Pane) projectSelectionNodeMap.get(p);
      final Label projectNameLabel = (Label) pane.getChildren().get(0);
      final String tooltipText = createTooltipTextForProject(p);
      final Tooltip projectTooltip = projectNameLabel.getTooltip();
      if (projectTooltip == null) {
         projectNameLabel.setTooltip(new Tooltip());
      }
      projectNameLabel.getTooltip().setText(tooltipText);
   }

   private String createTooltipTextForProject(final Project p) {
      if (p.getDescription() == null || p.getDescription().isEmpty()) {
         return p.getName();
      } else {
         return String.format("%s%n%s", p.getName(), p.getDescription());
      }
   }

   private Dialog<Project> setupEditProjectDialog(final String title, final String headerText, final Project project) {
      final Dialog<Project> dialog = new Dialog<>();
      dialog.setTitle(title);
      dialog.setHeaderText(headerText);
      dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

      final GridPane grid = setUpEditProjectGridPane(project, dialog);

      // TODO disable OK button if no name is set
      dialog.getDialogPane().setContent(grid);

      return dialog;
   }

   private GridPane setUpEditProjectGridPane(final Project p, final Dialog<Project> dialog) {
      GridPane grid;
      final FXMLLoader loader = new FXMLLoader(Resources.getResource(RESOURCE.FXML_MANAGE_PROJECT));
      loader.setControllerFactory(model.getSpringContext()::getBean);
      try {
         grid = loader.load();
      } catch (final IOException e) {
         throw new FXMLLoaderException("Error while loading '" + Resources.RESOURCE.FXML_MANAGE_PROJECT + "'.", e);
      }
      final ManageProjectController manageProjectController = loader.getController();
      manageProjectController.initializeWith(p);

      dialog.setResultConverter(dialogButton -> {
         if (dialogButton == ButtonType.OK) {
            return manageProjectController.getProjectFromUserInput();
         }
         return null;
      });

      return grid;
   }

   private ListCell<Project> returnListCellOfProject() {
      return new ListCell<Project>() {

         @Override
         protected void updateItem(final Project item, final boolean empty) {
            super.updateItem(item, empty);

            setText("");
            if (item == null || empty) {
               setGraphic(null);
            } else {
               Node graphic = projectSelectionNodeMap.get(item);
               if (graphic == null) {
                  final Project addedProject = item;
                  addProjectToProjectSelectionNodeMap(addedProject);
                  graphic = projectSelectionNodeMap.get(item);
               }
               LOG.trace("Item: '{}' -> '{}'", item.getName(), graphic);
               setGraphic(graphic);
            }
         }

      };
   }

}
