package application;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import application.Resources.RESOURCE;
import application.model.Project;
import application.model.repos.ProjectRepository;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@SpringBootApplication
public class DemoApplication extends Application {

   private ConfigurableApplicationContext springContext;
   private Parent root;

   private static final String CONFIG_FILE = "config.xml";

   public static Stage stage;

   public static List<Project> projects;
   public static Project idleProject;
   public static Color taskBarColor;

   private final Project defaultProject = new Project("Idle", Color.ORANGE, false);

   private final Logger Log = LoggerFactory.getLogger(this.getClass());

   @Override
   public void init() throws Exception {
      springContext = SpringApplication.run(DemoApplication.class);
   }

   @Override
   public void start(final Stage primaryStage) throws Exception {
      stage = primaryStage;

      final Test a = springContext.getBean(Test.class);
      Log.info("Test {}", a.projectRepository.count());

      try {
         Log.debug("Reading configuration");

         final ProjectRepository projectRepo = springContext.getBean(ProjectRepository.class);

         projects = projectRepo.findAll();

         Log.debug("Found '{}' projects", projects.size());

         if (projects.isEmpty()) {
            Log.info("Adding default project");
            defaultProject.isDefault = true;
            projects.add(defaultProject);
            projectRepo.save(defaultProject); // TODO autosave?
         }

         taskBarColor = Color.BLACK;// TODO read from config config.taskBarColor;
         Log.debug("Using color '{}' for taskbar.", taskBarColor);

         // set default project
         final Optional<Project> findAny = projects.stream().filter(p -> p.isDefault).findAny();
         if (findAny.isPresent()) {
            idleProject = findAny.get();
            Log.debug("Using project '{}' as default project.", idleProject);
         } else {
            Log.warn("No default project was found!");
            idleProject = defaultProject;

         }

      } catch (final Exception e) {
         final Alert alert = new Alert(AlertType.ERROR);
         alert.setTitle("Error");
         alert.setHeaderText("Could not read config file (" + CONFIG_FILE + ").");
         alert.setContentText("Please check your '" + CONFIG_FILE + "' file");

         final StringWriter sw = new StringWriter();
         final PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         final String exceptionText = sw.toString();

         final Label label = new Label("The exception stacktrace was:");

         final TextArea textArea = new TextArea(exceptionText);
         textArea.setEditable(false);
         textArea.setWrapText(true);

         textArea.setMaxWidth(Double.MAX_VALUE);
         textArea.setMaxHeight(Double.MAX_VALUE);
         GridPane.setVgrow(textArea, Priority.ALWAYS);
         GridPane.setHgrow(textArea, Priority.ALWAYS);

         final GridPane expContent = new GridPane();
         expContent.setMaxWidth(Double.MAX_VALUE);
         expContent.add(label, 0, 0);
         expContent.add(textArea, 0, 1);

         alert.getDialogPane().setExpandableContent(expContent);
         alert.showAndWait();
         System.exit(1);
      }

      Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run() {
            // Not on FX thread!
            shutdown();
         }
      });

      primaryStage.setOnCloseRequest((we) -> {
         // shutdown hook will take over
         System.exit(0);
      });

      try {
         initialiseLogin(primaryStage);
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }

   private void shutdown() {
      Log.info("Shutting down");
      mainController.changeProject(idleProject);
      try {
         Log.info("Creating summary.");
         mainController.createSummary();
         Log.info("Created summary");
      } catch (final IOException e) {
         Log.error("Error while creating summery :/", e);
      }
   }

   ViewController mainController;

   private void initialiseLogin(final Stage primaryStage) {
      try {
         Pane loginLayout;

         // Load root layout from fxml file.
         final FXMLLoader loader = new FXMLLoader();
         loader.setLocation(Resources.getResource(RESOURCE.FXML_VIEW_LAYOUT));
         loader.setControllerFactory(springContext::getBean);
         loginLayout = loader.load();
         primaryStage.initStyle(StageStyle.TRANSPARENT);
         // Show the scene containing the root layout.
         final Scene loginScene = new Scene(loginLayout, Color.TRANSPARENT);

         // Image(Resources.getResource(RESOURCE.ICON_MAIN).toString()));

         primaryStage.setTitle("KeepTime");
         primaryStage.setScene(loginScene);
         // Give the controller access to the main app.
         primaryStage.setAlwaysOnTop(true);
         mainController = loader.getController();
         mainController.setStage(primaryStage);
         // primaryStage.getIcons().add(new
         // Image(Resources.getResource(RESOURCE.IMG_XHODER_ICON).toString()));
         primaryStage.show();

      } catch (final Exception e) {
         Log.error("Error: " + e.toString(), e);
         e.printStackTrace();
      }
   }

   @Override
   public void stop() throws Exception {
      springContext.stop();
   }

   public static void main(final String[] args) {
      launch(DemoApplication.class, args);
   }
}
