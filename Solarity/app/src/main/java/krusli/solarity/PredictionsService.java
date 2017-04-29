package krusli.solarity;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by kenneth on 29/4/17.
 */

public interface PredictionsService {
    @GET("predictions")
    Observable<RadiationByHour> getPredictionsData(@Query("lat") float latitude, @Query("lon") float longitude,
                                                   @Query("month") float month, @Query("measurement") float lightIntensity);
}
