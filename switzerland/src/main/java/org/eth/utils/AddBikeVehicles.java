package org.eth.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.PersonVehicles;

public class AddBikeVehicles {
    public static void main(String[] args) {
        String configPath = "C:\\Users\\robig\\Desktop\\ABMT Git\\Project\\Data\\Lausanne_10pct\\lausanne_10pctconfig.xml"; //change to your config path
        String outputVehiclesFile = "C:\\Users\\robig\\Desktop\\ABMT Git\\Project\\Data\\Lausanne_10pct\\lausanne_10pct_vehicles_w_bike.xml"; // can name as you like and change to the path you want to save the file 
        String outputPopFile = "C:\\Users\\robig\\Desktop\\ABMT Git\\Project\\Data\\Lausanne_10pct\\lausanne_10pct_population_w_bike_vehicles.xml.gz";//= "..\\scenarios\\Lausanne_10pct\\new_vehicles.xml"; // can name as you like and change to the path you want to save the file 

        Config config = ConfigUtils.loadConfig(configPath);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Vehicles vehicles = scenario.getVehicles();
        String bikeMode = "bike";
        String ebike25Mode = "ebike25";
        String ebike45Mode = "ebike45";

        // We define a bike vehicle type
        VehicleType bikeType = VehicleUtils.createVehicleType(Id.create(bikeMode, VehicleType.class));
        bikeType.setMaximumVelocity(17.6 / 3.6); // can define yours based on your scenario
        bikeType.setPcuEquivalents(0.25); // can define yours
        bikeType.getCapacity().setSeats(1);

        if (!vehicles.getVehicleTypes().containsKey(bikeType.getId())) {
            vehicles.addVehicleType(bikeType);
        } else {
            bikeType = vehicles.getVehicleTypes().get(bikeType.getId());
        }

        // Define ebike25 vehicle type (25 km/h)
        VehicleType ebike25Type = VehicleUtils.createVehicleType(Id.create(ebike25Mode, VehicleType.class));
        ebike25Type.setMaximumVelocity(25.0 / 3.6); // 25 km/h
        ebike25Type.setPcuEquivalents(0.25);
        ebike25Type.getCapacity().setSeats(1);

        if (!vehicles.getVehicleTypes().containsKey(ebike25Type.getId())) {
            vehicles.addVehicleType(ebike25Type);
        } else {
            ebike25Type = vehicles.getVehicleTypes().get(ebike25Type.getId());
        }

        // Define ebike45 vehicle type (45 km/h)
        VehicleType ebike45Type = VehicleUtils.createVehicleType(Id.create(ebike45Mode, VehicleType.class));
        ebike45Type.setMaximumVelocity(45.0 / 3.6); // 45 km/h
        ebike45Type.setPcuEquivalents(0.25);
        ebike45Type.getCapacity().setSeats(1);

        if (!vehicles.getVehicleTypes().containsKey(ebike45Type.getId())) {
            vehicles.addVehicleType(ebike45Type);
        } else {
            ebike45Type = vehicles.getVehicleTypes().get(ebike45Type.getId());
        }
        
        // Here we add bike or ebike vehicles for each person
        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (person.getId().toString().contains("freight")){
                continue;
            }

            // Check ebike availability first
            Object ebikeAvailability = person.getAttributes().getAttribute("ebikeAvailability");
            
            if (ebikeAvailability != null && !ebikeAvailability.toString().equals("NO_EBIKE")) {
                // Person has an ebike - set bikeAvailability to FOR_NONE
                person.getAttributes().putAttribute("bikeAvailability", "FOR_NONE");
                
                String vehicleMode;
                VehicleType vehicleType;
                
                if (ebikeAvailability.toString().equals("EBIKE25")) {
                    vehicleMode = ebike25Mode;
                    vehicleType = ebike25Type;
                } else { // EBIKE45
                    vehicleMode = ebike45Mode;
                    vehicleType = ebike45Type;
                }
                
                Id<Vehicle> vehicle_id = Id.createVehicleId(person.getId().toString() + ":" + vehicleMode);
                Vehicle ebikeVehicle = VehicleUtils.createVehicle(vehicle_id, vehicleType);
                vehicles.addVehicle(ebikeVehicle);
                
                // Update vehicles in the population file
                Object vehicle_attr = person.getAttributes().getAttribute("vehicles");
                PersonVehicles personVehicles;

                if (vehicle_attr == null) {
                    personVehicles = new PersonVehicles();
                } else {
                    personVehicles = (PersonVehicles) vehicle_attr;
                }

                personVehicles.addModeVehicle(vehicleMode, vehicle_id);
                person.getAttributes().putAttribute("vehicles", personVehicles);
                
            } else {
                // Check if person has bike availability attribute (FOR_SOME or FOR_ALL)
                Object bikeAvailability = person.getAttributes().getAttribute("bikeAvailability");
                if (bikeAvailability != null && 
                    (bikeAvailability.toString().equals("FOR_SOME") || bikeAvailability.toString().equals("FOR_ALL"))) {
                    Id<Vehicle> vehicle_id = Id.createVehicleId(person.getId().toString() + ":" + bikeMode);
                    Vehicle bikeVehicle = VehicleUtils.createVehicle(vehicle_id, bikeType);
                    vehicles.addVehicle(bikeVehicle);
                    
                    // Update vehicles in the population file
                    Object vehicle_attr = person.getAttributes().getAttribute("vehicles");
                    PersonVehicles personVehicles;

                    if (vehicle_attr == null) {
                        personVehicles = new PersonVehicles();
                    } else {
                        personVehicles = (PersonVehicles) vehicle_attr;
                    }

                    personVehicles.addModeVehicle(bikeMode, vehicle_id);
                    person.getAttributes().putAttribute("vehicles", personVehicles);
                }
            }
            
        }

        new MatsimVehicleWriter(vehicles).writeFile(outputVehiclesFile);
        new PopulationWriter(scenario.getPopulation()).write(outputPopFile);
    }
}