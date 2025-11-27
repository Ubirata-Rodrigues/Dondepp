package com.example.dondepp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dondepp.adapters.PlacesAdapter;
import com.example.dondepp.model.Place;
import com.example.dondepp.services.OverpassService;
import com.example.dondepp.utils.LocationHelper;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    //Componentes visuais
    private MapView mapView;
    private RecyclerView recyclerViewPlaces;
    private EditText searchEditText;
    private ImageButton btnCurrentLocation;
    private ProgressBar progressBar;
    private TextView tvResultsCount;
    private TextView tvNoResults;
    private CardView bottonSheet;

    // Botoes de categoria
    private MaterialButton btnPharmacy;
    private MaterialButton btnRestaurant;
    private MaterialButton btnCafe;
    private MaterialButton btnSupermarket;
    private MaterialButton btnHospital;

    // Adapter e dados
    private PlacesAdapter placesAdapter;
    private List<Place> currentPlaces;
    private List<Marker> mapMarkers;

    // Mapa e localizacao
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private double currentLatitude = 0;
    private double currentLongitude = 0;
    private LocationHelper locationHelper;

    // API
    private OverpassService overpassService;

    // Botton Sheet
    private BottomSheetBehavior bottomSheetBehavior;

    // Permissoes
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        // Inicializar componentes
        initializeViews();
        initializeMap();
        initializeRecyclerView();
        initializeRetrofit();
        initializeLocationHelper();

        // Configurar listeners
        setupListeners(); // criar metodo

        // Pedir permissoes de localizacao
        checkLocationPermission();

    }

    private void initializeViews() {

        // Mapa
        mapView = findViewById(R.id.mapView);

        // RecyclerView e Bottom Sheet
        recyclerViewPlaces = findViewById(R.id.recyclerViewPlaces);
        bottonSheet = findViewById(R.id.bottomSheet);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        tvNoResults = findViewById(R.id.tvNoResults);

        // Busca
        searchEditText = findViewById(R.id.searchEditText);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);

        // Loading
//        progressBar = findViewById(R.id.progressBar);

        // Botoes de categoria
        btnPharmacy = findViewById(R.id.btnPharmacy);
        btnRestaurant = findViewById(R.id.btnRestaurant);
        btnCafe = findViewById(R.id.btnCafe);
        btnSupermarket = findViewById(R.id.btnSupermarket);
        btnHospital = findViewById(R.id.btnHospital);

        // Configurar Botton Sheet Behavior
        bottomSheetBehavior = BottomSheetBehavior.from(bottonSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(200);

        // Inicializar listas
        currentPlaces = new ArrayList<>();
        mapMarkers = new ArrayList<>();

    }

    public void initializeMap() {
        // Configurar mapa
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        // Controller do mapa
        mapController = mapView.getController();
        mapController.setZoom(15.0);

        // Localizacao padrao (Bsb)
        GeoPoint startPoint = new GeoPoint(-15.7942, -47.8822);
        mapController.setCenter(startPoint);

        // Overlay de localizacao do usuario
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);
        mapView.getOverlayManager().add(myLocationOverlay);
        mapView.invalidate();
    }

    public void initializeRecyclerView() {
        placesAdapter = new PlacesAdapter(this);
        recyclerViewPlaces.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPlaces.setAdapter(placesAdapter);

        // Listener para cliques nos lugares
        placesAdapter.setOnPlaceClickListener(new PlacesAdapter.OnPlaceClickListener() {
            @Override
            public void onPlaceClick(Place place) {
                // Centralizar mapa no lugar clicado
                GeoPoint point = new GeoPoint(place.getLatitude(), place.getLongitude());
                mapController.animateTo(point);
                mapController.setZoom(17.0);
            }

            @Override
            public void onUberClick(Place place) {
                Toast.makeText(MainActivity.this, "Abrindo Uber...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://overpass-api.de/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        overpassService = retrofit.create(OverpassService.class);
    }

    private void initializeLocationHelper() {
        locationHelper = new LocationHelper(this);
    }

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida
                getCurrentLocation();
            } else {
                // Permissão negada
                Toast.makeText(this, "Permissão de localização necessária", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getCurrentLocation() {
        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentLatitude = latitude;
                currentLongitude = longitude;

                // Centralizar mapa na localização atual
                GeoPoint userLocation = new GeoPoint(latitude, longitude);
                mapController.setCenter(userLocation);
                mapController.setZoom(15.0);

                Toast.makeText(MainActivity.this, "Localização obtida!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationError(String error) {
                Toast.makeText(MainActivity.this, "Erro: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

}