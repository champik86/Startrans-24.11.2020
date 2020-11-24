package com.example.startrans.data;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.startrans.R;

import java.util.ArrayList;

public class UserOrderAdapter extends RecyclerView.Adapter <UserOrderAdapter.RecyclerViewViewHolder> {

    ArrayList <UserOrder> arrayList;
    Context context;

    class RecyclerViewViewHolder extends RecyclerView.ViewHolder
    implements View.OnClickListener {

        public TextView tvUserOrderID, tvUserOrderDirection, tvUserOrderDate;

        public RecyclerViewViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            tvUserOrderID = itemView.findViewById(R.id.tvUserOrderID);
            tvUserOrderDirection = itemView.findViewById(R.id.tvUserOrderDirection);
            tvUserOrderDate = itemView.findViewById(R.id.tvUserOrderDate);
        }

        @Override
        public void onClick(View v) {

            int position = getAdapterPosition();
            UserOrder userOrder = arrayList.get(position);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Информация о заявке")
                        .setMessage(userOrder.getDateAndTime()+"\n"+
                                userOrder.getFrom()+"\n"+
                                userOrder.getTo()+"\n"+
                                "Тип автомобиля: "+userOrder.getTypeOfVehicle()+"\n"+
                                "Масса: "+userOrder.getWeight()+"\n"+
                                "Объем: "+userOrder.getSize()+"\n"+
                                "Примечания: "+userOrder.getAdds()+".")
                        .setPositiveButton("OK", null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
        }
    }

    public UserOrderAdapter (ArrayList<UserOrder> arrayList, Context context) {
        this.arrayList = arrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.user_order_item, parent, false);
        RecyclerViewViewHolder recyclerViewViewHolder= new RecyclerViewViewHolder(view);
        return recyclerViewViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewViewHolder holder, int position) {

        UserOrder userOrder = arrayList.get(position);

        holder.tvUserOrderID.setText(String.valueOf(userOrder.getId()));
        holder.tvUserOrderDirection.setText(userOrder.getFrom()+"\n"+ userOrder.getTo());
        holder.tvUserOrderDate.setText(userOrder.getDateAndTime());
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

}
