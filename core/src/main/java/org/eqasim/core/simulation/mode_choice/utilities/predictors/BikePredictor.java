package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class BikePredictor extends CachedVariablePredictor<BikeVariables> {
	@Override
	public BikeVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
    // Find the actual bike leg in the elements
    double travelTime_min = 0.0;
    
    for (PlanElement element : elements) {
        if (element instanceof Leg) {
            Leg leg = (Leg) element;
            if (leg.getMode().equals("bike") || leg.getMode().equals("ebike")) {
                travelTime_min = leg.getTravelTime().seconds() / 60.0;
                break;
            }
        }
    }
    return new BikeVariables(travelTime_min);
}
}
