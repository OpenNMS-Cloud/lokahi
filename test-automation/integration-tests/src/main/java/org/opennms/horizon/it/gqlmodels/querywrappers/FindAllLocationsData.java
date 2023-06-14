package org.opennms.horizon.it.gqlmodels.querywrappers;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;
import org.opennms.horizon.it.gqlmodels.LocationData;

public class FindAllLocationsData {

    private Wrapper data;

    public Wrapper getData() {
        return data;
    }

    public void setData(Wrapper data) {
        this.data = data;
    }

    public static class Wrapper {
        private List<LocationData> findAllLocations;

        public List<LocationData> getFindAllLocations() {
            return findAllLocations;
        }

        public void setFindAllLocations(List<LocationData> findAllLocations) {
            this.findAllLocations = findAllLocations;
        }
    }
}
