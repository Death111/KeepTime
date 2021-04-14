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

package de.doubleslash.keeptime.common;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import org.junit.Test;


public class DateFormatterTest {

   @Test
   public void durationShouldBePositiveWhenStartBeforeEndDate() {
      final LocalDateTime startDate = LocalDateTime.now();
      final LocalDateTime endDate = startDate.plusSeconds(10);

      final long secondsBewtween = DateFormatter.getSecondsBewtween(startDate, endDate);
      assertThat(secondsBewtween, Matchers.is(10l));
   }
   
   @Test
   public void durationShouldBePositiveWhenEndBeforeStartDate() {
	   final LocalDateTime startDate = LocalDateTime.now();
	   final LocalDateTime endDate = startDate.plusSeconds(10);
	   
	   
	   final long secondsBewtweenSwitched = DateFormatter.getSecondsBewtween(endDate, startDate);
	   assertThat(secondsBewtweenSwitched, Matchers.is(10l));
   }
}
