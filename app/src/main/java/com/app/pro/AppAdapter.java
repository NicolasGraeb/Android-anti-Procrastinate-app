package com.app.pro;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    public interface OnAppBlockedStateChangedListener {
        void onBlockedStateChanged(String packageName, boolean isBlocked);
    }

    private final Context context;
    private final List<AppInfo> appList;
    private OnAppBlockedStateChangedListener listener; // Listener

    public AppAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
    }

    public void setOnAppBlockedStateChangedListener(OnAppBlockedStateChangedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == RecyclerView.NO_POSITION) {
            Log.w("AppAdapter", "Invalid position in onBindViewHolder: " + position);
            return;
        }
        AppInfo currentApp = appList.get(position);


        holder.appNameTextView.setText(currentApp.getName());
        holder.appIconImageView.setImageDrawable(currentApp.getIcon());

        holder.checkBoxBlocked.setOnCheckedChangeListener(null);
        holder.checkBoxBlocked.setChecked(currentApp.isBlocked());

        holder.checkBoxBlocked.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int bindingPosition = holder.getBindingAdapterPosition();
            if (bindingPosition != RecyclerView.NO_POSITION) {
                AppInfo appInfo = appList.get(bindingPosition);
                appInfo.setBlocked(isChecked); // Aktualizuj dane
                Log.d("AppAdapter", "Checkbox changed for: " + appInfo.getPackageName() + " to " + isChecked);
                if (listener != null) {
                    listener.onBlockedStateChanged(appInfo.getPackageName(), isChecked);
                }
            } else {
                Log.w("AppAdapter", "Checkbox listener fired for invalid position.");
            }
        });
        holder.itemView.setOnClickListener(v -> {
            holder.checkBoxBlocked.toggle();
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIconImageView;
        TextView appNameTextView;
        CheckBox checkBoxBlocked;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIconImageView = itemView.findViewById(R.id.imageViewAppIcon);
            appNameTextView = itemView.findViewById(R.id.textViewAppName);
            checkBoxBlocked = itemView.findViewById(R.id.checkBoxBlocked);
        }
    }
}