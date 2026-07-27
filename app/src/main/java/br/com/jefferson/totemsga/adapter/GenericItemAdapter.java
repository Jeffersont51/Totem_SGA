package br.com.jefferson.totemsga.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayoutManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.jefferson.totemsga.R;
import br.com.jefferson.totemsga.util.IconMapper;
import android.content.res.ColorStateList;
import android.widget.ImageView;
import android.widget.FrameLayout;

public class GenericItemAdapter extends RecyclerView.Adapter<GenericItemAdapter.ViewHolder> {

    public static final int STYLE_HORIZONTAL = 0;
    public static final int STYLE_VERTICAL = 1;
    public static final int STYLE_SCHEDULING = 2;
    public static final int STYLE_REPRINT = 3;

    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

    private List<?> items;
    private OnItemClickListener listener;
    private String primaryColor;
    private Map<String, String> itemColors;
    private Map<String, String> itemTextColors;
    private Map<Integer, Boolean> expiredItems = new HashMap<>();
    private int selectedId = -1;
    private int spanCount = 2;
    private int viewStyle = STYLE_HORIZONTAL;

    public GenericItemAdapter(List<?> items, String primaryColor, Map<String, String> itemColors, Map<String, String> itemTextColors, OnItemClickListener listener) {
        this(items, primaryColor, itemColors, itemTextColors, STYLE_HORIZONTAL, listener);
    }

    public GenericItemAdapter(List<?> items, String primaryColor, Map<String, String> itemColors, Map<String, String> itemTextColors, int viewStyle, OnItemClickListener listener) {
        this.items = items;
        this.primaryColor = primaryColor;
        this.itemColors = itemColors != null ? itemColors : new HashMap<>();
        this.itemTextColors = itemTextColors != null ? itemTextColors : new HashMap<>();
        this.viewStyle = viewStyle;
        this.listener = listener;
    }

    public void setSpanCount(int spanCount) {
        this.spanCount = spanCount;
        notifyDataSetChanged();
    }

    public void setSelectedId(int id) {
        this.selectedId = id;
        notifyDataSetChanged();
    }

    public void setExpiredItems(Map<Integer, Boolean> expiredItems) {
        this.expiredItems = expiredItems != null ? expiredItems : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        if (viewStyle == STYLE_SCHEDULING || viewStyle == STYLE_REPRINT) {
            layout = R.layout.item_agendamento_card;
        } else {
            layout = (viewStyle == STYLE_HORIZONTAL) ? R.layout.item_selection_card_horizontal : R.layout.item_selection_card_vertical;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = items.get(position);
        
        if (viewStyle == STYLE_SCHEDULING && item instanceof br.com.jefferson.totemsga.model.Agendamento) {
            bindScheduling(holder, (br.com.jefferson.totemsga.model.Agendamento) item);
            return;
        }
        
        if (viewStyle == STYLE_REPRINT && item instanceof br.com.jefferson.totemsga.model.SenhaFila) {
            bindReprint(holder, (br.com.jefferson.totemsga.model.SenhaFila) item);
            return;
        }

        String name = "";
        int id = -1;
        int iconRes = -1;
        boolean showChevron = false;
        
        if (item instanceof br.com.jefferson.totemsga.model.Departamento) {
            br.com.jefferson.totemsga.model.Departamento d = (br.com.jefferson.totemsga.model.Departamento) item;
            name = d.nome;
            id = d.id;
            iconRes = IconMapper.getDepartmentIcon(name);
            showChevron = true;
        } else if (item instanceof br.com.jefferson.totemsga.model.ServicoUnidade) {
            br.com.jefferson.totemsga.model.ServicoUnidade s = (br.com.jefferson.totemsga.model.ServicoUnidade) item;
            name = s.servico.nome;
            id = s.servico.id;
            iconRes = IconMapper.getServiceIcon(name);
        } else if (item instanceof br.com.jefferson.totemsga.model.Prioridade) {
            br.com.jefferson.totemsga.model.Prioridade p = (br.com.jefferson.totemsga.model.Prioridade) item;
            name = p.nome;
            id = p.id;
            iconRes = R.drawable.ic_health_cross; // Default for priority
        }

        holder.tvName.setText(name);
        
        String itemColor = itemColors.get(String.valueOf(id));
        if (viewStyle == STYLE_HORIZONTAL) {
            if (itemColor == null || itemColor.equalsIgnoreCase(primaryColor)) {
                // Alternating Palette: Yellow, Orange, Red
                String[] palette = {"#FFCC00", "#F47B20", "#E31E24"};
                itemColor = palette[position % palette.length];
            }
        } else {
            if (itemColor == null) itemColor = primaryColor;
        }

        if (selectedId != -1) {
            if (id != selectedId) {
                // Unselected state - use gray
                itemColor = "#DDDDDD";
            }
        }
        
        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (lp instanceof FlexboxLayoutManager.LayoutParams) {
            FlexboxLayoutManager.LayoutParams flexLp = (FlexboxLayoutManager.LayoutParams) lp;
            float percent = 1.0f / spanCount;
            flexLp.setFlexBasisPercent(percent - 0.01f); 
            flexLp.setFlexGrow(1.0f);
        }

        try {
            int color = Color.parseColor(itemColor);
            
            if (viewStyle == STYLE_HORIZONTAL) {
                // Neutral background with color accents
                holder.cardView.setCardBackgroundColor(Color.WHITE);
                
                if (holder.vAccentBar != null) {
                    holder.vAccentBar.setVisibility(View.VISIBLE);
                    holder.vAccentBar.setBackgroundColor(color);
                }

                holder.ivIcon.setImageResource(iconRes);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE)); // White icon on colored background
                
                // Colored circle background for icon
                holder.flIconContainer.setBackgroundTintList(ColorStateList.valueOf(color));
                holder.flIconContainer.setAlpha(1.0f);
                
                holder.tvName.setTextColor(Color.parseColor("#333333"));
                if (holder.ivChevron != null) {
                    holder.ivChevron.setVisibility(showChevron ? View.VISIBLE : View.GONE);
                    holder.ivChevron.setImageTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
                }
            } else {
                // White background, tinted icon (Grid style)
                holder.cardView.setCardBackgroundColor(Color.WHITE);
                holder.ivIcon.setImageResource(iconRes);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(color));
                
                // Solid Light tint background for icon container
                // We'll calculate a light version of the color without alpha on the view
                int lightColor = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color));
                holder.flIconContainer.setBackgroundTintList(ColorStateList.valueOf(lightColor));
                holder.flIconContainer.setAlpha(1.0f); // Solid view
                
                holder.tvName.setTextColor(Color.parseColor("#333333"));
            }
            
            // Selection indicator (Stroke)
            if (selectedId != -1 && id == selectedId) {
                holder.cardView.setStrokeWidth(6);
                holder.cardView.setStrokeColor(viewStyle == STYLE_HORIZONTAL ? Color.WHITE : color);
            } else {
                holder.cardView.setStrokeWidth(0);
            }
            
        } catch (Exception e) {}
        
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                    v.performClick();
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    break;
            }
            return true;
        });

        // Set click listener as normal, but touch listener handles the timing
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    private void bindScheduling(ViewHolder holder, br.com.jefferson.totemsga.model.Agendamento a) {
        if (holder.tvServiceName != null) holder.tvServiceName.setText(a.servico != null ? a.servico.nome : "Atendimento");
        if (holder.tvScheduleTime != null) holder.tvScheduleTime.setText(a.hora);
        if (holder.tvClientName != null) holder.tvClientName.setText(a.cliente != null ? a.cliente.nome : "Cliente");
        if (holder.tvScheduleDate != null) holder.tvScheduleDate.setText("Data: " + a.data);

        boolean isExpired = expiredItems.containsKey(a.id) && Boolean.TRUE.equals(expiredItems.get(a.id));

        if (holder.cardAgendamento != null) {
            if (isExpired) {
                holder.cardAgendamento.setStrokeWidth(4);
                holder.cardAgendamento.setStrokeColor(Color.RED);
            } else if (selectedId != -1 && a.id == selectedId) {
                holder.cardAgendamento.setStrokeWidth(6);
                holder.cardAgendamento.setStrokeColor(Color.parseColor("#2ECC71")); // Green for selected
            } else {
                holder.cardAgendamento.setStrokeWidth(2);
                holder.cardAgendamento.setStrokeColor(Color.parseColor("#DDDDDD"));
            }
            holder.cardAgendamento.setOnClickListener(v -> listener.onItemClick(a));
        }
    }

    private void bindReprint(ViewHolder holder, br.com.jefferson.totemsga.model.SenhaFila s) {
        if (holder.tvServiceName != null) holder.tvServiceName.setText(s.servico != null ? s.servico.nome : "Senha");
        if (holder.tvScheduleTime != null) holder.tvScheduleTime.setText(s.senha != null ? s.senha.format : "");
        if (holder.tvClientName != null) holder.tvClientName.setText(s.cliente != null ? s.cliente.nome : "Cliente");
        if (holder.tvScheduleDate != null) holder.tvScheduleDate.setText("Emitida em: " + s.dataChegada);

        if (holder.cardAgendamento != null) {
            if (selectedId != -1 && s.id == selectedId) {
                holder.cardAgendamento.setStrokeWidth(6);
                holder.cardAgendamento.setStrokeColor(Color.parseColor("#F47B20")); // Orange for selected in reprint
            } else {
                holder.cardAgendamento.setStrokeWidth(2);
                holder.cardAgendamento.setStrokeColor(Color.parseColor("#DDDDDD"));
            }
            holder.cardAgendamento.setOnClickListener(v -> listener.onItemClick(s));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvServiceName, tvScheduleTime, tvClientName, tvScheduleDate;
        com.google.android.material.card.MaterialCardView cardView, cardAgendamento;
        ImageView ivIcon, ivChevron;
        FrameLayout flIconContainer;
        View vAccentBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            cardView = itemView.findViewById(R.id.cardSelection);
            ivIcon = itemView.findViewById(R.id.ivItemIcon);
            ivChevron = itemView.findViewById(R.id.ivChevron);
            flIconContainer = itemView.findViewById(R.id.flIconContainer);
            vAccentBar = itemView.findViewById(R.id.vAccentBar);

            // Scheduling specific fields
            cardAgendamento = itemView.findViewById(R.id.cardAgendamento);
            tvServiceName = itemView.findViewById(R.id.tvServiceName);
            tvScheduleTime = itemView.findViewById(R.id.tvScheduleTime);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvScheduleDate = itemView.findViewById(R.id.tvScheduleDate);
        }
    }
}
