/*
 * Copyright 2008 Brian Ferris
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.washington.cs.rse.transit.phone.actions.schedule;

import org.springframework.stereotype.Component;

import edu.washington.cs.rse.transit.common.model.StopLocation;
import edu.washington.cs.rse.transit.web.oba.common.client.model.StopWithArrivalsBean;

@Component
public class MostRecentAction extends AbstractPredictedScheduleAction {

    private static final long serialVersionUID = 1L;

    @Override
    public String execute() throws Exception {

        StopLocation lastSelection = _bookmarkService.getLastSelection(_userId);

        if (lastSelection == null)
            return INPUT;
        
        String stopId = Integer.toString(lastSelection.getId());

        StopWithArrivalsBean bean = _obaService.getArrivalsByStopId(stopId);
        _predictions = bean.getPredictedArrivals();
        return SUCCESS;
    }
}
