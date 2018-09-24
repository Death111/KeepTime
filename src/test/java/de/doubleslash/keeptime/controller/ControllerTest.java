package de.doubleslash.keeptime.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.doubleslash.keeptime.common.DateProvider;
import de.doubleslash.keeptime.model.Model;
import de.doubleslash.keeptime.model.Project;
import de.doubleslash.keeptime.model.Work;
import de.doubleslash.keeptime.model.repos.ProjectRepository;
import de.doubleslash.keeptime.model.repos.SettingsRepository;
import de.doubleslash.keeptime.model.repos.WorkRepository;
import javafx.scene.paint.Color;

public class ControllerTest {

   private Controller testee;

   private Model model;
   private DateProvider mockedDateProvider;

   @Before
   public void beforeTest() {
      model = new Model(Mockito.mock(ProjectRepository.class), Mockito.mock(WorkRepository.class),
            Mockito.mock(SettingsRepository.class));
      mockedDateProvider = Mockito.mock(DateProvider.class);
      testee = new Controller(model, mockedDateProvider);
   }

   @Test
   public void moveProjectFromEndToStart() {

      final List<Integer> expectedOrderAfter = Arrays.asList(1, 2, 3, 0);

      final Project project0 = new Project();
      project0.setIndex(0);
      final Project project1 = new Project();
      project1.setIndex(1);
      final Project project2 = new Project();
      project2.setIndex(2);

      final Project project3 = new Project();
      final int oldIndex = 3;
      project3.setIndex(oldIndex);
      final int newIndex = 0;
      project3.setIndex(newIndex);

      // change index to 0
      final List<Project> projectList = Arrays.asList(project0, project1, project2, project3);
      testee.resortProjectIndexes(projectList, project3, oldIndex, newIndex);

      for (int i = 0; i < projectList.size(); i++) {
         assertThat(projectList.get(i).getIndex(), Matchers.is(expectedOrderAfter.get(i)));
      }

   }

   @Test
   public void moveProjectFromStartToEnd() {

      final List<Integer> expectedOrderAfter = Arrays.asList(3, 0, 1, 2);

      final Project project0 = new Project();
      final int oldIndex = 0;
      project0.setIndex(oldIndex);
      final int newIndex = 3;
      project0.setIndex(newIndex);

      final Project project1 = new Project();
      project1.setIndex(1);
      final Project project2 = new Project();
      project2.setIndex(2);
      final Project project3 = new Project();
      project3.setIndex(3);

      final List<Project> projectList = Arrays.asList(project0, project1, project2, project3);
      testee.resortProjectIndexes(projectList, project0, oldIndex, newIndex);

      for (int i = 0; i < projectList.size(); i++) {
         assertThat(projectList.get(i).getIndex(), Matchers.is(expectedOrderAfter.get(i)));
      }

   }

   @Test
   public void moveProjectForward() {

      final List<Integer> expectedOrderAfter = Arrays.asList(0, 2, 1, 3);

      final Project project0 = new Project();
      project0.setIndex(0);

      final Project project1 = new Project();
      final int oldIndex = 1;
      project1.setIndex(oldIndex);
      final int newIndex = 2;
      project1.setIndex(newIndex);
      final Project project2 = new Project();
      project2.setIndex(2);
      final Project project3 = new Project();
      project3.setIndex(3);

      final List<Project> projectList = Arrays.asList(project0, project1, project2, project3);
      testee.resortProjectIndexes(projectList, project1, oldIndex, newIndex);

      for (int i = 0; i < projectList.size(); i++) {
         assertThat(projectList.get(i).getIndex(), Matchers.is(expectedOrderAfter.get(i)));
      }

   }

   @Test
   public void moveProjectBackward() {

      final List<Integer> expectedOrderAfter = Arrays.asList(0, 2, 1, 3);

      final Project project0 = new Project();
      project0.setIndex(0);

      final Project project1 = new Project();
      project1.setIndex(1);
      final Project project2 = new Project();
      final int oldIndex = 2;
      project2.setIndex(oldIndex);
      final int newIndex = 1;
      project2.setIndex(newIndex);
      final Project project3 = new Project();
      project3.setIndex(3);

      final List<Project> projectList = Arrays.asList(project0, project1, project2, project3);
      testee.resortProjectIndexes(projectList, project2, oldIndex, newIndex);

      for (int i = 0; i < projectList.size(); i++) {
         assertThat(projectList.get(i).getIndex(), is(expectedOrderAfter.get(i)));
      }
   }

   @Test
   public void dontMoveProjectTest() {

      final List<Integer> expectedOrderAfter = Arrays.asList(0, 1, 2, 3);

      final Project project0 = new Project();
      project0.setIndex(0);

      final Project project1 = new Project();
      project1.setIndex(1);
      final Project project2 = new Project();
      project2.setIndex(2);
      final Project project3 = new Project();
      project3.setIndex(3);

      final List<Project> projectList = Arrays.asList(project0, project1, project2, project3);
      for (final Project project : projectList) {
         testee.resortProjectIndexes(projectList, project, project.getIndex(), project.getIndex());
      }

      for (int i = 0; i < projectList.size(); i++) {
         assertThat(projectList.get(i).getIndex(), is(expectedOrderAfter.get(i)));
      }
   }

   @Test
   public void changeProjectSameDayTest() {
      final LocalDateTime firstProjectDateTime = LocalDateTime.now();
      final LocalDate firstProjectDate = firstProjectDateTime.toLocalDate();
      final LocalDateTime secondProjectDateTime = LocalDateTime.now();
      final LocalDate secondProjectDate = firstProjectDateTime.toLocalDate();

      Mockito.when(mockedDateProvider.dateTimeNow()).thenReturn(firstProjectDateTime);
      Mockito.when(mockedDateProvider.dateNow()).thenReturn(firstProjectDate);
      final Project firstProject = new Project("1st Project", Color.GREEN, true, 0);
      final Project secondProject = new Project("2nd Project", Color.RED, true, 1);
      testee.changeProject(firstProject);
      Mockito.when(mockedDateProvider.dateTimeNow()).thenReturn(secondProjectDateTime);
      Mockito.when(mockedDateProvider.dateNow()).thenReturn(secondProjectDate);
      testee.changeProject(secondProject);

      Mockito.verify(model.workRepository, Mockito.times(1)).save(Mockito.argThat((final Work savedWork) -> {
         if (savedWork.getProject() != firstProject) {
            return false;
         }
         if (!savedWork.getStartTime().equals(firstProjectDateTime)) {
            return false;
         }
         if (!savedWork.getEndTime().equals(secondProjectDateTime)) {
            return false;
         }
         return true;
      }));
      assertThat("Two project should be in the past work items", model.pastWorkItems.size(), is(2));
      assertThat("The first project should be the past work project", model.pastWorkItems.get(0).getProject(),
            is(firstProject));
      assertThat("The second project should be the active work project", model.activeWorkItem.get().getProject(),
            is(secondProject));
   }

   @Test
   public void changeProjectOtherDayTest() {
      final LocalDateTime firstProjectDateTime = LocalDateTime.now();
      final LocalDate firstProjectDate = firstProjectDateTime.toLocalDate();
      final LocalDateTime secondProjectDateTime = firstProjectDateTime.plusDays(1); // project is create the next day
      final LocalDate secondProjectDate = secondProjectDateTime.toLocalDate();

      Mockito.when(mockedDateProvider.dateTimeNow()).thenReturn(firstProjectDateTime);
      Mockito.when(mockedDateProvider.dateNow()).thenReturn(firstProjectDate);
      final Project firstProject = new Project("1st Project", Color.GREEN, true, 0);
      final Project secondProject = new Project("2nd Project", Color.RED, true, 1);
      testee.changeProject(firstProject);
      Mockito.when(mockedDateProvider.dateTimeNow()).thenReturn(secondProjectDateTime);
      Mockito.when(mockedDateProvider.dateNow()).thenReturn(secondProjectDate);
      testee.changeProject(secondProject);

      Mockito.verify(model.workRepository, Mockito.times(1)).save(Mockito.argThat((final Work savedWork) -> {
         if (savedWork.getProject() != firstProject) {
            return false;
         }
         if (!savedWork.getStartTime().equals(firstProjectDateTime)) {
            return false;
         }
         if (!savedWork.getEndTime().equals(secondProjectDateTime)) {
            return false;
         }
         return true;
      }));
      assertThat("One projects should be in the past work items", model.pastWorkItems.size(), is(1));
      assertThat("The project should be the second work project", model.pastWorkItems.get(0).getProject(),
            is(secondProject));
      assertThat("The second project should be the active work project", model.activeWorkItem.get().getProject(),
            is(secondProject));
   }

}
