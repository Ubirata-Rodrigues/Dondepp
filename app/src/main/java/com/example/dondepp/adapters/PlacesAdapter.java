package com.example.dondepp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dondepp.R;
import com.example.dondepp.model.Place;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {

    private List<Place> places;
    private Context context;
    private OnPlaceClickListener listener;

    // Interface para cliques no item
    public interface OnPlaceClickListener {
        void onPlaceClick(Place place);
        void onUberClick(Place place);
    }

    // Construtor
    public PlacesAdapter(Context context) {
        this.context = context;
        this.places = new ArrayList<>();
    }

    // Definir listener de cliques
    public void setOnPlaceClickListener(OnPlaceClickListener listener) {
        this.listener = listener;
    }

    // Atualizar lista de lugares
    public void updatePlaces(List<Place> newPlaces) {
        this.places.clear();
        this.places.addAll(newPlaces);
        notifyDataSetChanged();
    }

    // Limpar lista
    public void clearPlaces() {
        this.places.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        Place place = places.get(position);
        holder.bind(place);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    // ViewHolder - representa cada item da lista
    class PlaceViewHolder extends RecyclerView.ViewHolder {

        TextView tvPlaceIcon;
        TextView tvPlaceName;
        TextView tvPlaceAddress;
        TextView tvPlaceDistance;
        MaterialButton btnUber;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);

            tvPlaceIcon = itemView.findViewById(R.id.tvPlaceIcon);
            tvPlaceName = itemView.findViewById(R.id.tvPlaceName);
            tvPlaceAddress = itemView.findViewById(R.id.tvPlaceAddress);
            tvPlaceDistance = itemView.findViewById(R.id.tvPlaceDistance);
            btnUber = itemView.findViewById(R.id.btnUber);
        }

        public void bind(Place place) {
            // Nome do lugar
            tvPlaceName.setText(place.getName());

            // Endereço (ou mensagem se não tiver)
            if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                tvPlaceAddress.setText(place.getAddress());
                tvPlaceAddress.setVisibility(View.VISIBLE);
            } else {
                tvPlaceAddress.setText("Endereço não disponível");
                tvPlaceAddress.setVisibility(View.VISIBLE);
            }

            // Distância
            tvPlaceDistance.setText(place.getFormattedDistance());

            // Ícone baseado no tipo de lugar
            tvPlaceIcon.setText(getIconForType(place.getType()));

            // Clique no item inteiro
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaceClick(place);
                }
            });

            // Clique no botão Uber
            btnUber.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUberClick(place);
                } else {
                    // Se não tiver listener, abrir Uber direto
                    openUber(place);
                }
            });
        }

        // Retorna emoji baseado no tipo de lugar
        private String getIconForType(String type) {
            if (type == null) return "📍";

            switch (type.toLowerCase()) {
                case "pharmacy":
                    return "💊";
                case "restaurant":
                    return "🍽️";
                case "cafe":
                    return "☕";
                case "supermarket":
                case "shop":
                    return "🛒";
                case "hospital":
                    return "🏥";
                case "fuel":
                case "gas_station":
                    return "⛽";
                case "bank":
                    return "🏦";
                case "atm":
                    return "💰";
                case "bar":
                case "pub":
                    return "🍺";
                case "fast_food":
                    return "🍔";
                case "bakery":
                    return "🥖";
                case "cinema":
                    return "🎬";
                case "school":
                    return "🏫";
                case "police":
                    return "👮";
                default:
                    return "📍";
            }
        }

        // Abrir Uber com destino específico
        // Podemos trocar essa implementacao para levar pro google maps ou waze
        // Ou ver um jeito de fazer a destino no proprio app
        private void openUber(Place place) {
            try {
                // Deep link do Uber
                // Formato: uber://?action=setPickup&pickup=my_location&dropoff[latitude]=X&dropoff[longitude]=Y
                String uberUri = String.format(
                        "uber://?action=setPickup&pickup=my_location&dropoff[latitude]=%f&dropoff[longitude]=%f&dropoff[nickname]=%s",
                        place.getLatitude(),
                        place.getLongitude(),
                        Uri.encode(place.getName())
                );

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uberUri));

                // Verificar se o app Uber está instalado
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                } else {
                    // Se não tiver Uber instalado, abrir na web
                    String webUrl = String.format(
                            "https://m.uber.com/ul/?action=setPickup&pickup=my_location&dropoff[latitude]=%f&dropoff[longitude]=%f",
                            place.getLatitude(),
                            place.getLongitude()
                    );
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                    context.startActivity(webIntent);
                }

            } catch (Exception e) {
                Toast.makeText(context, "Erro ao abrir Uber: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
