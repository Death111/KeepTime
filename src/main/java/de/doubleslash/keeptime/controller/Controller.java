package de.doubleslash.keeptime.controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.doubleslash.keeptime.common.DateFormatter;
import de.doubleslash.keeptime.common.DateProvider;
import de.doubleslash.keeptime.model.Model;
import de.doubleslash.keeptime.model.Project;
import de.doubleslash.keeptime.model.Settings;
import de.doubleslash.keeptime.model.Work;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

@Service
public class Controller {

   private static final Logger LOG = LoggerFactory.getLogger(Controller.class);

   private final Model model;

   private final DateProvider dateProvider;

   @Autowired
   public Controller(final Model model, final DateProvider dateProvider) {
      this.model = model;
      this.dateProvider = dateProvider;
   }

   public void changeProject(final Project newProject) {
      changeProject(newProject, 0);
   }

   public void changeProject(final Project newProject, final long minusSeconds) {
      final Work currentWork = Model.activeWorkItem.get();

      final LocalDateTime now = dateProvider.dateTimeNow().minusSeconds(minusSeconds);
      final LocalDate dateNow = now.toLocalDate();
      if (currentWork != null) {
         currentWork.setEndTime(now);
         if (currentWork.getNotes().isEmpty()) {
            currentWork.setNotes("- No notes -");
         }

         final String time = DateFormatter
               .secondsToHHMMSS(Duration.between(currentWork.getStartTime(), currentWork.getEndTime()).getSeconds());

         LOG.info("You worked from '{}' to '{}' ({}) on project '{}' with notes '{}'", currentWork.getStartTime(),
               currentWork.getEndTime(), time, currentWork.getProject().getName(), currentWork.getNotes());

         // Save in db
         model.getWorkRepository().save(currentWork);
      }

      // Start new work
      final Work work = new Work(dateNow, now, now.plusSeconds(minusSeconds), newProject, "");

      model.getPastWorkItems().add(work);
      if (currentWork != null && !dateNow.isEqual(currentWork.getCreationDate())) {
         LOG.info("Removing projects with other creation date than today '{}' from list.", dateNow);
         final int sizeBefore = model.getPastWorkItems().size();
         model.getPastWorkItems().removeIf(w -> !dateNow.isEqual(w.getCreationDate()));
         LOG.debug("Removed '{}' work items from past work items.", sizeBefore - model.getPastWorkItems().size());
      }
      Model.activeWorkItem.set(work);
   }

   public void addNewProject(final String projectName, final boolean isWork, final Color projectColor,
         final int index) {
      final Project project = new Project(projectName, projectColor, isWork, index, false);
      model.getAllProjects().add(project);
      model.getAvailableProjects().add(project);

      final List<Project> changedProjects = resortProjectIndexes(model.getAvailableProjects(), project,
            model.getAvailableProjects().size(), index);
      changedProjects.add(project);
      model.getProjectRepository().saveAll(changedProjects);
   }

   public void updateSettings(final Color hoverBackgroundColor, final Color hoverFontColor,
         final Color defaultBackgroundColor, final Color defaultFontColor, final Color taskBarColor,
         final boolean useHotkey, final boolean displayProjectsRight, final boolean hideProjectsOnMouseExit) {
      // TODO create holder for all the properties (or reuse Settings.class?)
      final Settings settings = model.getSettingsRepository().findAll().get(0);
      settings.setTaskBarColor(taskBarColor);

      settings.setDefaultBackgroundColor(defaultBackgroundColor);
      settings.setDefaultFontColor(defaultFontColor);

      settings.setHoverBackgroundColor(hoverBackgroundColor);
      settings.setHoverFontColor(hoverFontColor);
      settings.setUseHotkey(useHotkey);
      settings.setDisplayProjectsRight(displayProjectsRight);
      settings.setHideProjectsOnMouseExit(hideProjectsOnMouseExit);

      model.getSettingsRepository().save(settings);

      Model.DEFAULT_BACKGROUND_COLOR.set(settings.getDefaultBackgroundColor());
      Model.DEFAULT_FONT_COLOR.set(settings.getDefaultFontColor());
      Model.HOVER_BACKGROUND_COLOR.set(settings.getHoverBackgroundColor());
      Model.HOVER_FONT_COLOR.set(settings.getHoverFontColor());
      Model.TASK_BAR_COLOR.set(settings.getTaskBarColor());
      Model.USE_HOTKEY.set(settings.isUseHotkey());
      Model.DISPLAY_PROJECTS_RIGHT.set(settings.isDisplayProjectsRight());
      Model.HIDE_PROJECTS_ON_MOUSE_EXIT.set(settings.isHideProjectsOnMouseExit());
   }

   @PreDestroy
   public void shutdown() {
      LOG.info("Controller shutdown");
      changeProject(model.getIdleProject(), 0);
   }

   public void deleteProject(final Project p) {
      if (p.isDefault()) {
         LOG.error("You cannot delete the default project. Tried to delete project '{}'", p);
         return;
      }
      LOG.info("Disabeling project '{}'.", p);

      final int indexToRemove = p.getIndex();
      p.setEnabled(false); // we don't delete it because of the referenced work
                           // items
      p.setIndex(-1);

      model.getAvailableProjects().remove(p);

      final List<Project> changedProjects = adaptProjectIndexesAfterRemoving(model.getAvailableProjects(),
            indexToRemove);

      changedProjects.add(p);
      model.getProjectRepository().saveAll(changedProjects);
   }

   public void editProject(final Project p, final String newName, final Color newColor, final boolean isWork,
         final int newIndex) {
      LOG.info("Changing project '{}' to '{}' '{}' '{}'", p, newName, newColor, isWork);

      p.setName(newName);
      p.setColor(newColor);
      p.setWork(isWork);
      final int oldIndex = p.getIndex();
      p.setIndex(newIndex);

      final List<Project> changedProjects = resortProjectIndexes(model.getAvailableProjects(), p, oldIndex, newIndex);
      changedProjects.add(p);

      // save all projects which changed index
      model.getProjectRepository().saveAll(changedProjects);
   }

   /**
    * Changes the indexes of the originalList parameter to have a consistent order.
    * 
    * @param originalList
    *           list of all projects to adapt the indexes for
    * @param changedProject
    *           the project which has changed which already has the new index
    * @param oldIndex
    *           the old index of the changed project
    * @param newIndex
    *           the new index of the changed project (which the projects also already has)
    * @return all projects whose index has been adapted
    */
   List<Project> resortProjectIndexes(final List<Project> originalList, final Project changedProject,
         final int oldIndex, final int newIndex) {
      final ArrayList<Project> changedProjects = new ArrayList<>(Math.abs(oldIndex - newIndex));
      if (newIndex == oldIndex) {
         return changedProjects;
      }

      final boolean newIndexGreater = newIndex > oldIndex;
      final int adjustOffset = newIndexGreater ? -1 : +1;

      final int smallerIndex = Math.min(newIndex, oldIndex);
      final int biggerIndex = Math.max(newIndex, oldIndex);

      for (int i = 0; i < originalList.size(); i++) {
         final Project project = originalList.get(i);
         final int currentIndex = project.getIndex();
         if ((currentIndex < smallerIndex || currentIndex > biggerIndex) || project == changedProject) {
            // index is not affected by change or
            // this one is already at the right/wanted index
            continue;
         }

         final int newCurrentIndex = currentIndex + adjustOffset;
         project.setIndex(newCurrentIndex);
         changedProjects.add(project);
      }

      return changedProjects;
   }

   /**
    * Decreases all indexes by one, after the removed index
    * 
    * @param originalList
    *           list of all projects to adapt the indexes for
    * @param removedIndex
    *           the index which has been removed
    * @return all projects whose index has been adapted
    */
   List<Project> adaptProjectIndexesAfterRemoving(final List<Project> originalList, final int removedIndex) {
      final int size = originalList.size();
      final List<Project> changedProjects = new ArrayList<>(size - removedIndex);
      for (int i = 0; i < size; i++) {
         final Project project = originalList.get(i);
         if (project.getIndex() < removedIndex) {
            // Not affected
            continue;
         }

         project.setIndex(project.getIndex() - 1);
         changedProjects.add(project);
      }
      return changedProjects;
   }

   public void setComment(final String notes) {
      final Work work = Model.activeWorkItem.get();
      work.setNotes(notes);
   }

   /**
    * Calculate todays seconds counted as work
    */
   public long calcTodaysWorkSeconds() {
      final List<Work> workItems = new ArrayList<>();

      for (final Work work : model.getPastWorkItems()) {
         final Project project = work.getProject();
         for (final Project p : model.getAllProjects()) {
            if (p.getId() == project.getId() && project.isWork()) {
               workItems.add(work);
            }
         }
      }

      return calcSeconds(workItems);
   }

   /**
    * Calculate todays present seconds (work+nonWork)
    */
   public long calcTodaysSeconds() {
      return calcSeconds(model.getPastWorkItems());
   }

   public ObservableList<Project> getAvailableProjects() {
      return model.getAvailableProjects();
   }

   public long calcSeconds(final List<Work> workItems) {
      long seconds = 0;

      for (final Work w : workItems) {
         seconds += Duration.between(w.getStartTime(), w.getEndTime()).getSeconds();
      }

      return seconds;
   }
}
