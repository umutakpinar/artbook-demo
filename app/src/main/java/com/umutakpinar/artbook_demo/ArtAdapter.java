package com.umutakpinar.artbook_demo;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.umutakpinar.artbook_demo.databinding.RecycleRowBinding;

import java.util.ArrayList;

public class ArtAdapter extends RecyclerView.Adapter<ArtAdapter.ArtHolder> {

    private ArrayList<Art> arrayList;

    public ArtAdapter(ArrayList<Art> _arrayList) {
        this.arrayList = _arrayList;
    }

    @NonNull
    @Override
    public ArtHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecycleRowBinding recycleRowBinding = RecycleRowBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new ArtHolder(recycleRowBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtHolder holder, int position) {
        holder.binding.recyclerViewTextView.setText(arrayList.get(position).name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(holder.itemView.getContext(),ArtActivity.class); //farklı calsstasın o nedenle bu şekilde context'e ulaştık
                intent.putExtra("info","old");
                intent.putExtra("artId",arrayList.get(holder.getAdapterPosition()).id); //burada direkt position kullanmadık! çünkü position değişkenmiş o nedenle
                holder.itemView.getContext().startActivity(intent); //3 satır önceki sebepten dolayı tekrar holder.itemView ile contexte ulaştık
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ArtHolder extends RecyclerView.ViewHolder{

        private RecycleRowBinding binding;

        public ArtHolder(RecycleRowBinding _binding) {
            super(_binding.getRoot());
            this.binding = _binding;
        }
    }
}




