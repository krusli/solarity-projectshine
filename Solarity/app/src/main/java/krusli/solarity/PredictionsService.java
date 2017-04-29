package krusli.solarity;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by kenneth on 29/4/17.
 */

public interface PredictionsService {
    @GET("predictions")
    Observable<ApiResponse> getPredictionsData(@Query("lat") double latitude, @Query("lon") double longitude,
                                               @Query("month") int month, @Query("measurement") float lightIntensity);
}
