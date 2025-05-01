
package com.app.pro;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private static final String TAG = "AppAdapter";

    public interface OnAppSettingsChangedListener {
        void onBlockedStateChanged(String packageName, boolean isBlocked);
        void onLimitChanged(String packageName, int totalMinutes);
    }

    private final Context context;
    private final List<AppInfo> appList;
    private OnAppSettingsChangedListener listener;

    public AppAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
    }

    public void setOnAppSettingsChangedListener(OnAppSettingsChangedListener listener) {
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
        if (position == RecyclerView.NO_POSITION) return;
        AppInfo currentApp = appList.get(position);

        holder.appNameTextView.setText(currentApp.getName());
        holder.appIconImageView.setImageDrawable(currentApp.getIcon());

        boolean isExpanded = currentApp.isExpanded();
        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.imageArrowExpand.setImageResource(isExpanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);

        if (isExpanded) {
            holder.checkBoxBlocked.setOnCheckedChangeListener(null);
            holder.checkBoxBlocked.setChecked(currentApp.isBlocked());
            holder.checkBoxBlocked.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    AppInfo appInfo = appList.get(currentPosition);
                    appInfo.setBlocked(isChecked);
                    Log.d(TAG, "Checkbox changed for: " + appInfo.getPackageName() + " to " + isChecked);
                    if (listener != null) {
                        listener.onBlockedStateChanged(appInfo.getPackageName(), isChecked);
                    }
                } else {
                    Log.w(TAG, "Checkbox listener fired for invalid position.");
                }
            });

            int totalMinutes = currentApp.getDailyLimitMinutes();
            if (totalMinutes >= 0) {
                int hours = totalMinutes / 60;
                int minutes = totalMinutes % 60;
                holder.editTextHours.setText(String.format(Locale.getDefault(), "%d", hours));
                holder.editTextMinutes.setText(String.format(Locale.getDefault(), "%d", minutes));
            } else {
                holder.editTextHours.setText("");
                holder.editTextMinutes.setText("");
                holder.editTextHours.setHint("H");
                holder.editTextMinutes.setHint("Min");
            }
        }

        holder.buttonSaveLimit.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                AppInfo appInfo = appList.get(currentPosition);
                int hours = 0;
                int minutes = 0;
                try {
                    String hoursStr = holder.editTextHours.getText().toString();
                    String minutesStr = holder.editTextMinutes.getText().toString();

                    if (!TextUtils.isEmpty(hoursStr)) {
                        hours = Integer.parseInt(hoursStr);
                    }
                    if (!TextUtils.isEmpty(minutesStr)) {
                        minutes = Integer.parseInt(minutesStr);
                    }

                    if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
                        Toast.makeText(context, "Wprowadź poprawny czas (H: 0-23, Min: 0-59)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int newLimitMinutes = (hours == 0 && minutes == 0) ? -1 : (hours * 60 + minutes);
                    appInfo.setDailyLimitMinutes(newLimitMinutes);
                    listener.onLimitChanged(appInfo.getPackageName(), newLimitMinutes);
                    Toast.makeText(context, "Limit zapisany", Toast.LENGTH_SHORT).show();

                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Wprowadź poprawne liczby", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error parsing time limit numbers", e);
                }
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                AppInfo appInfo = appList.get(currentPosition);
                appInfo.setExpanded(!appInfo.isExpanded());
                notifyItemChanged(currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIconImageView;
        TextView appNameTextView;
        ImageView imageArrowExpand;
        LinearLayout expandableLayout;
        CheckBox checkBoxBlocked;
        EditText editTextHours;
        EditText editTextMinutes;
        Button buttonSaveLimit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIconImageView = itemView.findViewById(R.id.imageViewAppIcon);
            appNameTextView = itemView.findViewById(R.id.textViewAppName);
            imageArrowExpand = itemView.findViewById(R.id.imageArrowExpand);
            expandableLayout = itemView.findViewById(R.id.expandableLayout);
            checkBoxBlocked = itemView.findViewById(R.id.checkBoxBlocked);
            editTextHours = itemView.findViewById(R.id.editTextHours);
            editTextMinutes = itemView.findViewById(R.id.editTextMinutes);
            buttonSaveLimit = itemView.findViewById(R.id.buttonSaveLimit);
        }
    }
}

