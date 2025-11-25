package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;

import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.ElevationVariables;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.List;


public class ElevationPredictor extends CachedVariablePredictor<ElevationVariables> {
    private final Network network;

    @Inject
    public ElevationPredictor(Network network) {
        this.network = network;
    }

    @Override
    public ElevationVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
    Activity originActivity = trip.getOriginActivity();
    Activity destinationActivity = trip.getDestinationActivity();

    if (originActivity.getLinkId() == null || destinationActivity.getLinkId() == null) {
        return new ElevationVariables(0.0);
    }

    Link originLink = network.getLinks().get(originActivity.getLinkId());
    Link destinationLink = network.getLinks().get(destinationActivity.getLinkId());

    if (originLink == null || destinationLink == null) {
        return new ElevationVariables(0.0);
    }

    double originZ = originLink.getToNode().getCoord().getZ();
    double destinationZ = destinationLink.getToNode().getCoord().getZ();

    if (Double.isNaN(originZ) || Double.isNaN(destinationZ)) {
        return new ElevationVariables(0.0);
    }

    // Calculate euclidean distance
    double dx = destinationLink.getToNode().getCoord().getX() - originLink.getToNode().getCoord().getX();
    double dy = destinationLink.getToNode().getCoord().getY() - originLink.getToNode().getCoord().getY();
    double distance_m = Math.sqrt(dx * dx + dy * dy);

    // Calculate slope
    double elevationDiff_m = destinationZ - originZ;
    double slope = distance_m > 0 ? elevationDiff_m / distance_m : 0.0;

    return new ElevationVariables(slope);
    }
}   