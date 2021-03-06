package co.eventcloud.capetownweather.weather.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Object describing the Day's Weather Info we get back from the API. This object contains info about each hour of the day
 *
 * <p/>
 * Created by Laurie on 2017/08/10.
 */

@SuppressWarnings("unused")
public class DayWeatherInfo {

    @SerializedName("summary")
    @Expose
    private String summary;

    @SerializedName("icon")
    @Expose
    private String icon;

    @SerializedName("data")
    @Expose
    private List<HourInfo> data = null;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<HourInfo> getData() {
        return data;
    }

    public void setData(List<HourInfo> data) {
        this.data = data;
    }
}
