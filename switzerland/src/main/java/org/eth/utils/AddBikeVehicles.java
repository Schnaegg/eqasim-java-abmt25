package org.eth.utils;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;

import org.matsim.api.core.v01.population.Route;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.PersonVehicles;

public class AddBikeVehicles {
    public static void main(String[] args) {
        String configPath = "/Users/laura/Desktop/ABMT/Project/Data/Lausanne_10pct/lausanne_10pctconfig.xml"; // change to your
        String outputVehiclesFile = "/Users/laura/Desktop/ABMT/Project/Data/Lausanne_10pct/lausanne_10pctvehicles_w_bike.xml.gz";
        String outputPopFile = "/Users/laura/Desktop/ABMT/Project/Data/Lausanne_10pct/lausanne_10pctpopulation_w_bike_vehicles.xml.gz";

        Config config = ConfigUtils.loadConfig(configPath);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Vehicles vehicles = scenario.getVehicles();
        String bikeTypeId = "default_bike";
        String bikeMode = "bike";


        // We define a bike vehicle type
        VehicleType bikeType = VehicleUtils.createVehicleType(Id.create(bikeTypeId, VehicleType.class));
        bikeType.setMaximumVelocity(17.6 / 3.6); // can define yours based on your scenario
        bikeType.setPcuEquivalents(0.25); // can define yours
        bikeType.getCapacity().setSeats(1);
        bikeType.setNetworkMode(bikeMode);  // ADD THIS LINE
        bikeType.setLength(2.0); // ADD THIS LINE

        if (!vehicles.getVehicleTypes().containsKey(bikeType.getId())) {
            vehicles.addVehicleType(bikeType);
        } else {
            bikeType = vehicles.getVehicleTypes().get(bikeType.getId());
        }


        // Here we add bike vehicles for each person
        // First, identify all persons who actually have bike legs
        for (Person person : scenario.getPopulation().getPersons().values()) {
            // Skip freight agents
            if (person.getId().toString().contains("freight")) {
                continue;
            }

            // Check if person has any bike legs
            boolean hasBikeLeg = false;
            for (Plan plan : person.getPlans()) {
                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Leg) {
                        Leg leg = (Leg) element;
                        if (leg.getMode().equals("bike") || leg.getMode().equals("bike_loop")) {
                            hasBikeLeg = true;
                            break;
                        }
                    }
                }
                if (hasBikeLeg) break;
            }


            // Check bike availability attribute
            Object bikeAvailability = person.getAttributes().getAttribute("bikeAvailability");
            

            // Create bike vehicle if: bikeAvailability is not "FOR_NONE" OR person has bike legs
            boolean shouldHaveBike = (bikeAvailability != null && !bikeAvailability.toString().equals("FOR_NONE")) || hasBikeLeg;
    
            if (shouldHaveBike) {
                Id<Vehicle> vehicleId = Id.createVehicleId(person.getId().toString() + ":" + bikeMode);
                
                // Only add vehicle if it doesn't already exist
                if (!vehicles.getVehicles().containsKey(vehicleId)) {
                    Vehicle bikeVehicle = VehicleUtils.createVehicle(vehicleId, bikeType);
                    vehicles.addVehicle(bikeVehicle);
                }
                
                // Update vehicles attribute in population file
                Object vehicleAttr = person.getAttributes().getAttribute("vehicles");
                PersonVehicles personVehicles;

                if (vehicleAttr == null) {
                    personVehicles = new PersonVehicles();
                } else {
                    personVehicles = (PersonVehicles) vehicleAttr;
                }

                personVehicles.addModeVehicle(bikeMode, vehicleId);
                person.getAttributes().putAttribute("vehicles", personVehicles);
            }
        }
        // Remove routes for bike and bike_loop modes (let MATSim re-route them)
        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Leg) {
                        Leg leg = (Leg) element;
                        String mode = leg.getMode();
                        
                        // Remove routes for bike modes
                        if ((mode.equals("bike") || mode.equals("bike_loop")) && leg.getRoute() != null) {
                            leg.setRoute(null);  // Remove the route - MATSim will re-route
                        }
                    }
                }
            }
        }

        new MatsimVehicleWriter(vehicles).writeFile(outputVehiclesFile);
        new PopulationWriter(scenario.getPopulation()).write(outputPopFile);
    }
}