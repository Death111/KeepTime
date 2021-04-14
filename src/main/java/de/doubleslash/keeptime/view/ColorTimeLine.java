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

import java.time.Duration;
import java.util.List;

import de.doubleslash.keeptime.common.DateFormatter;
import de.doubleslash.keeptime.model.Work;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ColorTimeLine {

   private final Canvas canvas;

   public ColorTimeLine(final Canvas canvas) {
      this.canvas = canvas;
   }

   public void update(final List<Work> workItems, final long seconds) {
      final GraphicsContext gc = canvas.getGraphicsContext2D();

      gc.setFill(new Color(.3, .3, .3, .3));
      gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
      gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
      double currentX = 0;
      for (final Work w : workItems) {
         final long workedSeconds = DateFormatter.getSecondsBewtween(w.getStartTime(), w.getEndTime());
         final double width = (double) workedSeconds / seconds * canvas.getWidth();
         final Color fill = w.getProject().getColor();

         gc.setFill(fill);
         gc.fillRect(currentX, 0, width, canvas.getHeight());

         currentX += width;
      }

   }

}
