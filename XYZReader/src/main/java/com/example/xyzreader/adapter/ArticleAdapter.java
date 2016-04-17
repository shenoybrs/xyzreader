package com.example.xyzreader.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;


public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ViewHolder> {
    private Cursor mCursor;
    private Context context;
    private AdapterItemListener adapterItemListener;

    public interface AdapterItemListener {
        public void onItemClick(Uri uri, int position, View v);
    }

    public ArticleAdapter(Context context, AdapterItemListener adapterItemListener) {
        this.context = context;
        this.adapterItemListener = adapterItemListener;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_article, parent, false);
        final ViewHolder vh = new ViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (mCursor == null) {
            return;
        }
        mCursor.moveToPosition(position);
        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
        holder.subtitleView.setText(
                DateUtils.getRelativeTimeSpanString(
                        mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString());

        holder.articleAuthorView.setText(mCursor.getString(ArticleLoader.Query.AUTHOR));

        String url = mCursor.getString(ArticleLoader.Query.THUMB_URL);

        Picasso.with(context)
                .load(url)
                .into(holder.thumbnailView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.thumbnailView.setTransitionName(String.valueOf(position));
        }

        holder.thumbnailView.setTag(String.valueOf(position));
        holder.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adapterItemListener != null) {
                    adapterItemListener.onItemClick(ItemsContract.Items.buildItemUri(getItemId(position)), position, holder.thumbnailView);
                }
            }
        });
    }

    public void swapCursor(final Cursor cursor) {
        this.mCursor = cursor;
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.thumbnail)
        ImageView thumbnailView;
        @Bind(R.id.detail_article_title)
        TextView titleView;
        @Bind(R.id.article_subtitle)
        TextView subtitleView;
        @Bind(R.id.article_author)
        TextView articleAuthorView;
        View parent;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            this.parent = view;
        }
    }
}


