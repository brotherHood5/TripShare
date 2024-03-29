package com.example.tripshare.ui.search;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.tripshare.R;
import com.example.tripshare.model.User;

import java.util.List;

public class CustomSuggestionSearchUserAdapter extends ArrayAdapter<String> {
    Context context ;
    List<User> userListBelow;


    public CustomSuggestionSearchUserAdapter(@NonNull Context context, int layoutToBeInflated , List<User> userListBelow ) {
        super(context,R.layout.suggest_search_component,new String[userListBelow.size()]);
        this.userListBelow = userListBelow;

        this.context = context;
    }
    @Override
    public View getView(int position, View convertView , ViewGroup parent) {
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        View row = inflater.inflate(R.layout.suggest_search_component,null);
        TextView titleTextView = (TextView) row.findViewById(R.id.titleTextView);
        TextView subTiltleTextView = (TextView) row.findViewById(R.id.subTiltleTextView);
        ImageView icon_suggestion_search_imageview = ( ImageView ) row.findViewById(R.id.icon_suggestion_search_imageview);

        titleTextView.setText(userListBelow.get(position).getUserName());
        subTiltleTextView.setText(userListBelow.get(position).getNameNonAccent());
        Glide.with(row)
                .load(userListBelow.get(position).getAvatar())
                .placeholder(R.drawable.img_placeholder)
                .error(R.drawable.avatar)
                .into(icon_suggestion_search_imageview);

        return(row);
    }
}
