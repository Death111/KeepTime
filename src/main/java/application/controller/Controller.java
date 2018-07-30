package application.controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.Main;
import application.common.DateFormatter;
import application.model.Model;
import application.model.Project;
import application.model.Work;
import javafx.scene.paint.Color;

public class Controller implements IController {

   private final Logger Log = LoggerFactory.getLogger(this.getClass());

   private final Model model;

   public Controller(final Model model) {
      this.model = model;

   }

   @Override
   public void changeProject(final Project newProject, final String notes) {
      changeProject(newProject, notes, 0);
   }

   @Override
   public void changeProject(final Project newProject, final String notes, final int minusSeconds) {
      final Work currentWork = model.activeWorkItem.get();

      final LocalDateTime now = LocalDateTime.now();
      if (currentWork != null) {
         currentWork.setEndTime(now);
         currentWork.setNotes(notes);
         if (currentWork.getNotes().isEmpty()) {
            currentWork.setNotes("- No notes -");
         }

         final String time = DateFormatter
               .secondsToHHMMSS(Duration.between(currentWork.getStartTime(), currentWork.getEndTime()).getSeconds());
         Log.info("You worked from '{}' to '{}' ({}) on project '{}' with notes '{}'", currentWork.getStartTime(),
               currentWork.getEndTime(), time, currentWork.getProject().getName(), currentWork.getNotes());

         final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss");
         final String startTime = formatter1.format(currentWork.getStartTime());
         final String endTime = DateTimeFormatter.ofPattern("dd.MM.yyyy -  HH:mm:ss").format(currentWork.getEndTime());
         final String s = "------------------------------------\n" + currentWork.getProject().getName() + "\t"
               + startTime + " - " + endTime + "( " + time + " )" + "\n" + currentWork.getNotes() + "\n\n";

         // TODO Save in db
         // appendToFile(s);
         Main.workRepository.save(currentWork);
      }

      // Start new work
      final Work work = new Work(now, now, newProject, "");
      model.pastWorkItems.add(work);

      model.activeWorkItem.set(work);
   }

   @Override
   public void addNewProject(final String projectName, final boolean isWork, final Color projectColor) {
      final Project project = new Project(projectName, projectColor, isWork, false);
      // TODO save new project into db
      model.availableProjects.add(project);

      Main.projectRepo.save(project);
   }

   @Override
   public Object getDetails(final LocalDate date) {
      // XXX Auto-generated method stub
      return null;
   }

   @Override
   public void setColors(final Object colors) {
      // XXX Auto-generated method stub

   }

   @Override
   public void shutdown() {
      // XXX Auto-generated method stub

   }

}
