package tk.urbantaxi.dtxi;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tk.urbantaxi.dtxi.classes.Requestor;
import tk.urbantaxi.dtxi.models.VehicleModel;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Vehicles extends AppCompatActivity {
    TextView t;
    public final static String VEHICLE_URL = "http://urbantaxi.tk/vehicle_images/";

    public Integer PAGE = 1;
    public List<VehicleModel> vehicleModelList = new ArrayList();
    public GridView gvVehicles;
    public VehicleAdapter adapter;
    public ProgressBar pbVehicles;
    public Boolean checker = false;
    public TextView tvErrorMessage;
    public SwipeRefreshLayout swipeRefreshLayout;

    public Requestor requestor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicles);

        Map<String, Object> param = new LinkedHashMap<>();
        requestor = new Requestor("vehicle", param, this){

            @Override
            public void onNetworkError(){
                postExecute(true, "");
            }

            @Override
            public void cancelled(){
                postExecute(true, "");
            }

            @Override
            public void preExecute(){
                pbVehicles.setVisibility(View.VISIBLE);
            }

            @Override
            public void postExecute(Boolean cancelled, String s){
                pbVehicles.setVisibility(View.GONE);
                if(swipeRefreshLayout.isRefreshing()){
                    swipeRefreshLayout.setRefreshing(false);
                }
                if(cancelled == false){
                    try {
                        JSONObject result = new JSONObject(s);
                        JSONArray vehicles = result.getJSONArray("data");
                        if(vehicles.length() == 0){
                            if(vehicleModelList.size() > 0){
                                Snackbar.make(gvVehicles, "No more data to be shown.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                            }else{
                                tvErrorMessage.setText("No data found. Tap to retry.");
                                gvVehicles.setEmptyView(tvErrorMessage);
                                setAdapter();
                            }
                        }else{
                            PAGE++;
                            requestor.setPage(PAGE);
                            for(int i = 0; i < vehicles.length(); i++){
                                JSONObject vehicle = vehicles.getJSONObject(i);
                                VehicleModel vehicleModel = new VehicleModel();
                                vehicleModel.setCode(vehicle.getString("code"));
                                vehicleModel.setImage(vehicle.getString("image"));
                                vehicleModelList.add(vehicleModel);
                            }
                            setAdapter();
                        }


                    } catch (JSONException e) {
                        if(vehicleModelList.size() > 0){
                            Snackbar.make(gvVehicles, "Something went wrong. Please try again later.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        }else{
                            tvErrorMessage.setText("Something went wrong. Please try again later. Tap to retry.");
                            gvVehicles.setEmptyView(tvErrorMessage);
                            setAdapter();
                        }
                    }
                }else if(cancelled == true){
                    if(vehicleModelList.size() > 0){
                        Snackbar.make(gvVehicles, "Please make sure you are connected to the internet.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }else{
                        tvErrorMessage.setText("Please make sure you are connected to the internet. Tap to retry.");
                        gvVehicles.setEmptyView(tvErrorMessage);
                        setAdapter();
                    }
                }
            }
        };

        t = (TextView) findViewById(R.id.vehicleText);
        Typeface myCustomFont = Typeface.createFromAsset(getAssets(),"fonts/Raleway-Regular.ttf");
        t.setTypeface(myCustomFont);

        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc(true).cacheInMemory(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .displayer(new FadeInBitmapDisplayer(300)).build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                getApplicationContext())
                .defaultDisplayImageOptions(defaultOptions)
                .memoryCache(new WeakMemoryCache())
                .discCacheSize(100 * 1024 * 1024).build();

        ImageLoader.getInstance().init(config);

        gvVehicles = (GridView) findViewById(R.id.gvVehicles);
        pbVehicles = (ProgressBar) findViewById(R.id.pbVehicles);
        tvErrorMessage = (TextView) findViewById(R.id.tvEmpty);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);

        tvErrorMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pbVehicles.setVisibility(View.VISIBLE);
                requestor.execute();
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                PAGE = 1;
                requestor.setPage(PAGE);
                vehicleModelList.clear();
                requestor.execute();
            }
        });
        requestor.setPage(PAGE);
        requestor.execute();
    }

    public void setAdapter(){
        if(adapter == null){
            adapter = new VehicleAdapter(this, R.layout.vehiclegrid, vehicleModelList);
            gvVehicles.setAdapter(adapter);
            gvVehicles.setOnScrollListener(new AbsListView.OnScrollListener() {
                private int mLastFirstVisibleItem;
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    int threshold = 1;
                    int count = gvVehicles.getCount();

                    if (scrollState == SCROLL_STATE_IDLE) {
                        if (gvVehicles.getLastVisiblePosition() >= count
                                - threshold) {
                            requestor.execute();
                        }
                    }
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                }
            });

            gvVehicles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getApplicationContext(), Startsession.class);
                    intent.putExtra("code", vehicleModelList.get(position).getCode());
                    intent.putExtra("image", vehicleModelList.get(position).getImage());
                    startActivity(intent);
                }
            });
        }else{
            adapter.notifyDataSetChanged();
        }
    }

    public class VehicleAdapter extends ArrayAdapter {
        private List<VehicleModel> vehicleModel;
        private int resource;
        private LayoutInflater inflater;
        private ImageLoader imageLoader = ImageLoader.getInstance();


        public VehicleAdapter(Context context, int resource, List<VehicleModel> objects) {
            super(context, resource, objects);
            this.vehicleModel = objects;
            this.resource = resource;
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if(convertView == null){
                holder = new ViewHolder();
                convertView = inflater.inflate(resource, null);
                holder.imgTaxi = (ImageView)convertView.findViewById(R.id.ivTaxi);
                holder.tvCode = (TextView)convertView.findViewById(R.id.tvCode);
                holder.pbImageProgress = (ProgressBar)convertView.findViewById(R.id.pbImageProgress);
                convertView.setTag(holder);
            }else{
                holder = (ViewHolder)convertView.getTag();
            }


            final ViewHolder finalHolder = holder;
            imageLoader.displayImage(VEHICLE_URL + vehicleModel.get(position).getImage(), holder.imgTaxi, new ImageLoadingListener(){
                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    finalHolder.pbImageProgress.setVisibility(View.VISIBLE);
                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    finalHolder.pbImageProgress.setVisibility(View.GONE);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    finalHolder.pbImageProgress.setVisibility(View.GONE);
                }

                @Override
                public void onLoadingCancelled(String imageUri, View view) {
                    finalHolder.pbImageProgress.setVisibility(View.GONE);
                }
            });
            holder.tvCode.setText(vehicleModel.get(position).getCode());

            return convertView;
        }

        class ViewHolder{
            private ImageView imgTaxi;
            private TextView tvCode;
            private ProgressBar pbImageProgress;
        }
    }
}
