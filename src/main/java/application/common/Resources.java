package application.common;

import java.net.URL;

public class Resources {

   public enum RESOURCE {
      /** LAYOUTS **/
      // main
      FXML_VIEW_LAYOUT("ViewLayout.fxml"),
      FXML_PROJECT_LAYOUT("ProjectDetailLayout.fxml"),
      FXML_SETTINGS("settings.fxml"),
      // icon
      ICON_MAIN("icon.png"),
      FXML_REPORT("report.fxml"),

      ;
      String resourceLocation;

      private RESOURCE(final String resourceLocation) {
         this.resourceLocation = resourceLocation;
      }

      public String getResourceLocation() {
         return resourceLocation;
      }
   }

   public static URL getResource(final RESOURCE resource) {
      return ClassLoader.getSystemResource(resource.getResourceLocation());
   }
}
