package com.example.dondepp;

import static com.google.android.material.internal.ViewUtils.hideKeyboard;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.example.dondepp.model.OverpassResponse;
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

import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    //Componentes visuais
    private MapView mapView;
    private RecyclerView recyclerViewPlaces;
    private EditText searchEditText;
    private ImageButton btnSearch;
    private ImageButton btnCurrentLocation;
    private ProgressBar progressBar;
    private TextView tvResultsCount;
    private TextView tvResultsTitle;
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
        btnSearch = findViewById(R.id.btnSearch);
        searchEditText = findViewById(R.id.searchEditText);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);

        // Loading
        progressBar = findViewById(R.id.progressBar);

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

    // Listeners
    private void setupListeners() {
        btnSearch.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                searchPlacesByText(query);
                hideKeyboard(); // esconder teclado apos busca
            } else {
                Toast.makeText(this, "Digite algo para buscar", Toast.LENGTH_SHORT).show();
            }
        });

        btnCurrentLocation.setOnClickListener(v -> {
            getCurrentLocation();
            if (currentLatitude != 0 && currentLongitude != 0) {
                GeoPoint userLocation = new GeoPoint(currentLatitude, currentLongitude);
                mapController.animateTo(userLocation);
                mapController.setZoom(15.0);
            }
        });
//
//        searchEditText.addTextChangedListener(new TextWatcher() {
//            private Handler handler = new Handler();
//            private Runnable runnable;
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {}
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                // Cancelar busca anterior
//                if (runnable != null) {
//                    handler.removeCallbacks(runnable);
//                }
//
//                // Aguardar 500ms antes de buscar (debounce)
//                runnable = () -> {
//                    String query = s.toString().trim();
//                    if (query.length() >= 3) { // Buscar apenas se tiver 3+ caracteres
//                        searchPlacesByText(query);
//                    }
//                };
//                handler.postDelayed(runnable, 500);
//            }
//
//        });

        btnPharmacy.setOnClickListener(v -> {
            searchPlacesByType("pharmacy", "Farmácias");
        });

        btnRestaurant.setOnClickListener(v -> {
            searchPlacesByType("restaurant", "Restaurantes");
        });

        btnCafe.setOnClickListener(v -> {
            searchPlacesByType("cafe", "Cafés");
        });

        btnSupermarket.setOnClickListener(v -> {
            searchPlacesByType("supermarket", "Mercados");
        });

        btnHospital.setOnClickListener(v -> {
            searchPlacesByType("hospital", "Hospitais");
        });

        // Busca por texto livre (Enter no teclado)
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                searchPlacesByText(query);
                return true;
            }
            return false;
        });

    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // Busca de lugares
    private void searchPlacesByType(String type, String typeName) {
        if(currentLatitude == 0 || currentLongitude == 0) {
            Toast.makeText(this, "Obtendo localizacao...", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
            return;
        }

         showLoading(true);

        String query = buildOverpassQuery(type, currentLatitude, currentLongitude, 2000);

        Call<OverpassResponse> call = overpassService.searchPlaces(query);
        call.enqueue(new Callback<OverpassResponse>() {
            @Override
            public void onResponse(Call<OverpassResponse> call, Response<OverpassResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<OverpassResponse.Element> elements = response.body().getElements();

                    if(elements != null && !elements.isEmpty()) {
                        List<Place> places = convertElementsToPlaces(elements, type);
                        displayResults(places, typeName);
                    } else {
                        showNoResults();
                        Toast.makeText(MainActivity.this, "Nenhum resultado encontrado para " + typeName, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<OverpassResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(MainActivity.this, "Erro ao buscar lugares: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPlacesByText(String searchText) {
        if (currentLatitude == 0 || currentLongitude == 0) {
            Toast.makeText(this, "Obtendo localizacao...", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
            return;
        }

        showLoading(true);

        String query = buildOverpassQueryByName(searchText, currentLatitude, currentLongitude, 2000);
        Call<OverpassResponse> call = overpassService.searchPlaces(query);
        call.enqueue(new Callback<OverpassResponse>() {
            @Override
            public void onResponse(Call<OverpassResponse> call, Response<OverpassResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<OverpassResponse.Element> elements = response.body().getElements();

                    if (elements != null && !elements.isEmpty()) {
                        List<Place> places = convertElementsToPlaces(elements, "search");
                        displayResults(places, "Resultados");
                    } else {
                        showNoResults();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Erro na busca", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OverpassResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(MainActivity.this, "Erro de conexão", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String buildOverpassQuery(String amenityType, double lat, double lon, int radius) {
        return String.format(
                "[out:json];(node[\"amenity\"=\"%s\"](around:%d,%.6f,%.6f);way[\"amenity\"=\"%s\"](around:%d,%.6f,%.6f););out center;",
                amenityType, radius, lat, lon,
                amenityType, radius, lat, lon
        );
    }

    private String buildOverpassQueryByName(String name, double lat, double lon, int radius) {
        return String.format(
                "[out:json];(node[\"name\"~\"%s\",i](around:%d,%.6f,%.6f);way[\"name\"~\"%s\",i](around:%d,%.6f,%.6f););out center;",
                name, radius, lat, lon,
                name, radius, lat, lon
        );
    }

    // Converter e processar dados
    private List<Place> convertElementsToPlaces(List<OverpassResponse.Element> elements, String type) {
        List<Place> places = new ArrayList<>();

        for (OverpassResponse.Element element : elements) {
            Place place = new Place(
                    element.getName(),
                    element.getLat(),
                    element.getLon(),
                    type
            );

            // Add endereco se disponivel
            String address = element.getAddress();
            if(address != null && !address.isEmpty()) {
                place.setAddress(address);
            }

            // Calcula distancia do usuario
            double distance = LocationHelper.calculateDistance(
                    currentLatitude, currentLongitude,
                    element.getLat(), element.getLon()
            );
            place.setDistance(distance);

            places.add(place);
        }

        // Ordenar por distancia (mais proximo primeiro)
        Collections.sort(places, new Comparator<Place>() {
            @Override
            public int compare(Place o1, Place o2) {
                return Double.compare(o1.getDistance(), o2.getDistance());
            }
        });

        return places;
    }

    // Exibir Resultados

    private void displayResults(List<Place> places, String categoryName) {
        currentPlaces = places;

        placesAdapter.updatePlaces(places);

        tvResultsCount.setText(places.size() + " lugares");
        tvResultsTitle.setText(categoryName);

        tvNoResults.setVisibility(View.GONE);
        recyclerViewPlaces.setVisibility(View.VISIBLE);

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

// Adicionar marcadores no mapa
        addMarkersToMap(places);

        // Ajustar zoom do mapa para mostrar todos os lugares
        if (!places.isEmpty()) {
            adjustMapBounds(places);
        }
    }

    private void showNoResults() {
        placesAdapter.clearPlaces();
        tvResultsCount.setText("0 lugares");
        tvNoResults.setVisibility(View.VISIBLE);
        recyclerViewPlaces.setVisibility(View.GONE);
        clearMapMarkers();
    }

// ==================== MARCADORES NO MAPA ====================

    private void addMarkersToMap(List<Place> places) {
        // Limpar marcadores antigos
        clearMapMarkers();

        // Adicionar novo marcador para cada lugar
        for (Place place : places) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(place.getLatitude(), place.getLongitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(place.getName());
            marker.setSnippet(place.getAddress() + "\n" + place.getFormattedDistance());

            // Customizar ícone baseado no tipo (opcional)
            // marker.setIcon(getDrawable(R.drawable.ic_marker));

            mapView.getOverlays().add(marker);
            mapMarkers.add(marker);
        }

        // Redesenhar mapa
        mapView.invalidate();
    }

    private void clearMapMarkers() {
        for (Marker marker : mapMarkers) {
            mapView.getOverlays().remove(marker);
        }
        mapMarkers.clear();
        mapView.invalidate();
    }

    private void adjustMapBounds(List<Place> places) {
        if (places.isEmpty()) return;

        // Encontrar limites (bounding box)
        double minLat = places.get(0).getLatitude();
        double maxLat = places.get(0).getLatitude();
        double minLon = places.get(0).getLongitude();
        double maxLon = places.get(0).getLongitude();

        for (Place place : places) {
            minLat = Math.min(minLat, place.getLatitude());
            maxLat = Math.max(maxLat, place.getLatitude());
            minLon = Math.min(minLon, place.getLongitude());
            maxLon = Math.max(maxLon, place.getLongitude());
        }

        // Incluir posição do usuário
        minLat = Math.min(minLat, currentLatitude);
        maxLat = Math.max(maxLat, currentLatitude);
        minLon = Math.min(minLon, currentLongitude);
        maxLon = Math.max(maxLon, currentLongitude);

        // Calcular centro
        double centerLat = (minLat + maxLat) / 2;
        double centerLon = (minLon + maxLon) / 2;

        // Centralizar mapa
        mapController.setCenter(new GeoPoint(centerLat, centerLon));

        // Ajustar zoom (aproximado)
        double latDiff = maxLat - minLat;
        double lonDiff = maxLon - minLon;
        double maxDiff = Math.max(latDiff, lonDiff);

        double zoom = 15.0;
        if (maxDiff > 0.1) zoom = 12.0;
        else if (maxDiff > 0.05) zoom = 13.0;
        else if (maxDiff > 0.02) zoom = 14.0;

        mapController.setZoom(zoom);
    }

// ==================== UTILIDADES ====================

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    }





















