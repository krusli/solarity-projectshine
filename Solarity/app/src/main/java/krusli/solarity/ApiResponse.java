package krusli.solarity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by kenneth on 29/4/17.
 */

public class ApiResponse {

    @SerializedName("radiationByHour")
    @Expose
    private List<Float> radiationByHour = null;

    public List<Float> getRadiationByHour() {
        return radiationByHour;
    }

    public void setRadiationByHour(List<Float> radiationByHour) {
        this.radiationByHour = radiationByHour;
    }

}
