package co.eventcloud.capetownweather.weather;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.thbs.skycons.library.SkyconView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import co.eventcloud.capetownweather.BuildConfig;
import co.eventcloud.capetownweather.R;
import co.eventcloud.capetownweather.manager.WeatherManager;
import co.eventcloud.capetownweather.realm.dao.WeatherDao;
import co.eventcloud.capetownweather.realm.model.RealmCurrentWeatherInfo;
import co.eventcloud.capetownweather.utils.IconUtil;
import co.eventcloud.capetownweather.utils.UiUtil;
import co.eventcloud.capetownweather.utils.callback.FadeOutAnimationCompletedCallback;
import co.eventcloud.capetownweather.weather.callback.WeatherUpdateListener;
import co.eventcloud.capetownweather.weather.event.WeatherInfoUpdateErrorEvent;
import co.eventcloud.capetownweather.weather.event.WeatherInfoUpdatedEvent;
import io.realm.Realm;

/**
 * Fragment showing the current weather
 *
 * <p/>
 * Created by Laurie on 2017/08/11.
 */

@SuppressWarnings({"CanBeFinal", "unused"})
public class CurrentWeatherFragment extends Fragment {

    @BindView(R.id.summary)
    TextView summary;
    @BindView(R.id.real_temperature)
    TextView realTemperature;
    @BindView(R.id.skycon_placeholder)
    FrameLayout skyconPlaceholder;
    @BindView(R.id.apparent_temperature)
    TextView apparentTemperature;
    @BindView(R.id.precipitation)
    TextView precipitation;
    @BindView(R.id.humidity)
    TextView humidity;
    @BindView(R.id.wind_speed)
    TextView windSpeed;

    @BindView(R.id.time)
    TextView time;
    @BindView(R.id.shimmerFrameLayout)
    ShimmerFrameLayout shimmerFrameLayout;

    // Keep reference to the current weather DB object
    private RealmCurrentWeatherInfo currentWeatherInfo;

    // Butterknife unbinder
    private Unbinder unbinder;

    /**
     * The swipe to refresh layout, that will do the GET weather API call once swiped down
     */
    @BindView(R.id.swipeToRefresh)
    SwipeRefreshLayout swipeToRefreshLayout;
    @BindView(R.id.sunLoadingIndicator)
    ImageView sunLoadingIndicator;
    @BindView(R.id.errorLayout)
    RelativeLayout errorLayout;
    @BindView(R.id.cardViewWeather)
    CardView cardViewWeather;
    @BindView(R.id.buttonTryAgain)
    Button buttonTryAgain;

    private Realm realm;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        realm = Realm.getDefaultInstance();

        currentWeatherInfo = WeatherDao.getCurrentWeatherInfo(realm);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_weather, container, false);

        unbinder = ButterKnife.bind(this, view);

        realm = Realm.getDefaultInstance();

        swipeToRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.colorAccent));

        // Set the refresh listener to do the GET weather call if swiped down
        swipeToRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showLoadingView();

                WeatherManager.getWeather(getContext(), new WeatherUpdateListener() {
                    @Override
                    public void onWeatherFinishedUpdating() {
                        hideLoadingView();
                    }

                    @Override
                    public void onWeatherUpdateError(String errorMessage) {
                        showErrorView(errorMessage);
                    }
                });
            }
        });

        setWeatherInfo();

        return view;
    }

    private void showLoadingView() {
        UiUtil.fadeViewOut(skyconPlaceholder, 250, new FadeOutAnimationCompletedCallback() {
            @Override
            public void onCompleted() {
                // Start the sun loading indicator
                sunLoadingIndicator.setVisibility(View.VISIBLE);
                UiUtil.startRotateForever(sunLoadingIndicator);

                // Start a progress indicator here as there is no data (this will only happen if the DB is empty, i.e. first launch of the app).
                // In this case, the weather is still busy being retrieved. Set the layout to show refreshing while this is happening.
                swipeToRefreshLayout.setRefreshing(true);

                // Start the shimmer loading indicator
                shimmerFrameLayout.startShimmerAnimation();
            }
        });
    }

    private void hideLoadingView() {

        // Disable refreshing of swipe to refresh layout
        if (swipeToRefreshLayout != null) {
            swipeToRefreshLayout.setRefreshing(false);
        }

        if (sunLoadingIndicator != null) {
            // Stop the rotating sun
            UiUtil.stopRotateForever(sunLoadingIndicator);
            sunLoadingIndicator.setVisibility(View.GONE);
        }

        if (shimmerFrameLayout != null) {
            // Stop the amazing shimmer animation
            shimmerFrameLayout.stopShimmerAnimation();
        }

        UiUtil.fadeViewIn(skyconPlaceholder);
    }

    private void showErrorView(final String errorMessage) {
        if (unbinder == null) {
            // If we get here the event has been received before Butterknife has been bound to the views
            // In this case, simply ignore it
            return;
        }

        if (getActivity() != null) {
            Snackbar.make(getActivity().findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_LONG).show();
        }

        hideLoadingView();

        // If there hasn't been any weather info saved to the DB, show the error card view
        if (currentWeatherInfo == null || currentWeatherInfo.getTemperature() == null) {
            if (cardViewWeather != null) {
                cardViewWeather.setVisibility(View.GONE);
            }

            if (errorLayout != null) {
                errorLayout.setVisibility(View.VISIBLE);
            }
        }

        if (buttonTryAgain != null) {
            buttonTryAgain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    showLoadingView();

                    WeatherManager.getWeather(getContext(), new WeatherUpdateListener() {
                        @Override
                        public void onWeatherFinishedUpdating() {
                            hideLoadingView();
                        }

                        @Override
                        public void onWeatherUpdateError(String errorMessage) {
                            showErrorView(errorMessage);
                        }
                    });
                }
            });
        }
    }

    private void setWeatherInfo() {
        if (unbinder == null) {
            // If we get here the event has been received before Butterknife has been bound to the views
            // In this case, simply ignore it
            return;
        }

        if (currentWeatherInfo != null) {
            // If for some reason the layout is still refreshing but we already have data, stop it
            if (swipeToRefreshLayout != null && swipeToRefreshLayout.isRefreshing()) {
                swipeToRefreshLayout.setRefreshing(false);
            }

            if (realm != null && realm.isClosed()) {
                realm = Realm.getDefaultInstance();
            }

            hideLoadingView();

            // Time is returned by API as seconds since epoch, convert it to millis
            long timeInMillis = currentWeatherInfo.getTime() * 1000L;

            // Create a date object from the timestamp
            Date date = new Date(timeInMillis);

            // Format the date using English locale and SAST timezone
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Africa/Johannesburg"));

            // Time
            if (time != null) {
                time.setText(String.format(getString(R.string.time), simpleDateFormat.format(date)));
            }

            // Summmary
            if (summary != null) {
                summary.setText(currentWeatherInfo.getSummary());
                summary.setSelected(true);
            }

            // Real Temp
            if (realTemperature != null) {
                String temperature = String.format(getString(R.string.temperature), currentWeatherInfo.getTemperature().intValue());
                realTemperature.setText(temperature);
            }

            // Apparent Temp
            if (apparentTemperature != null) {
                String apparentTemperatureString = String.format(getString(R.string.apparentTemperature), currentWeatherInfo.getApparentTemperature().intValue());
                apparentTemperature.setText(apparentTemperatureString);
            }

            // Humidity
            if (humidity != null) {
                final String humidityString = String.format(getString(R.string.humidity), (int) (currentWeatherInfo.getHumidity() * 100));
                humidity.setText(humidityString);
            }

            if (precipitation != null) {
                // Precipitation
                String precipitationString = String.format(getString(R.string.precipitation), (int) (currentWeatherInfo.getPrecipitationProbability() * 100));
                precipitation.setText(precipitationString);
            }

            if (windSpeed != null) {
                // Wind speed
                String windSpeedString = String.format(getString(R.string.windSpeed), (int) (currentWeatherInfo.getWindSpeed() * 3.6));
                windSpeed.setText(windSpeedString);
            }

            // Skycon!
            if (skyconPlaceholder != null) {
                String iconString = currentWeatherInfo.getIcon();

                final SkyconView skyconView = IconUtil.getSkyconView(getContext(), iconString, true);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                skyconView.setLayoutParams(params);

                // First remove any views that might be in the placeholder container
                if (skyconPlaceholder.getChildCount() > 0) {
                    skyconPlaceholder.removeAllViews();
                }

                // Now add the correct skycon view
                skyconPlaceholder.addView(skyconView);

                // Finally fade the view in
                UiUtil.fadeViewIn(skyconPlaceholder);
            }

            if (!realm.isClosed()) {
                realm.close();
            }
        } else {
            showLoadingView();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(WeatherInfoUpdatedEvent event) {
        realm = Realm.getDefaultInstance();

        // Get the updated weather info in the DB
        currentWeatherInfo = WeatherDao.getCurrentWeatherInfo(realm);

        // In debug mode only set the weather 3 seconds after the data has been received, because I like looking at the loading animations :)
        if (BuildConfig.DEBUG) {
            swipeToRefreshLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setWeatherInfo();
                }
            }, 3000);
        } else {
            setWeatherInfo();
        }

        // Now remove the sticky event, we don't want it to be handled again after navigating to this fragment
        EventBus.getDefault().removeStickyEvent(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(WeatherInfoUpdateErrorEvent event) {
        showErrorView(getString(R.string.error_generic));

        // Now remove the sticky event, we don't want it to be handled again after navigating to this fragment
        EventBus.getDefault().removeStickyEvent(event);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
