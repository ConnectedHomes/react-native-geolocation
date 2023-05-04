package co.uk.hive.reactnativegeolocation.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class DataMarshaller {

    private final Gson mGson;

    public DataMarshaller(Gson gson) {
        mGson = gson;
    }

    public <T> String marshal(T data) {
        return mGson.toJson(data);
    }

    public <T, C extends List<T>> C unmarshalList(String data, Class<T> listClassType, C defaultValue) {
        C result = mGson.fromJson(data, TypeToken.getParameterized(List.class, listClassType).getType());
        if (result != null) {
            return result;
        } else {
            return defaultValue;
        }
    }

    public <T> T unmarshal(String data, Class<T> classType, T defaultValue) {
        T result = mGson.fromJson(data, classType);
        if (result != null) {
            return result;
        } else {
            return defaultValue;
        }
    }
}
