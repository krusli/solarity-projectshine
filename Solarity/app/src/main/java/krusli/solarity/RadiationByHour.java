package krusli.solarity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by kenneth on 29/4/17.
 */

public class RadiationByHour {

    @SerializedName("radiationByHour")
    @Expose
    private List<Double> radiationByHour = null;

    public List<Double> getRadiationByHour() {
        return radiationByHour;
    }

    public void setRadiationByHour(List<Double> radiationByHour) {
        this.radiationByHour = radiationByHour;
    }

}
