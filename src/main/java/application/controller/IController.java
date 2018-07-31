package application.controller;

import application.model.Project;
import javafx.scene.paint.Color;

public interface IController {
   public void changeProject(Project newProject, String notes);

   public void changeProject(Project newProject, String notes, long minusSeconds);

   public void addNewProject(String projectName, boolean isWork, Color projectColor);

   /**
    * Colors for background and fonts
    */

   public void shutdown();

   public void deleteProject(Project p);

   public void editProject(Project p, String newName, Color newColor, boolean isWork);

   public void setColors(Color hoverBackgroundColor, Color hoverFontColor, Color defaultBackgroundColor,
         Color defaultFontColor, Color taskBarColor);

   public long calcTodaysSeconds();

   public long calcTodaysWorkSeconds();

}
