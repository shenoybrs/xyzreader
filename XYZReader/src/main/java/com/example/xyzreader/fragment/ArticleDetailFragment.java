package com.example.xyzreader.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.activity.ArticleDetailActivity;
import com.example.xyzreader.activity.ArticleListActivity;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import butterknife.Bind;
import butterknife.BindBool;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 *
 * Tnx to alexjlockwood for shared transition example for fragments
 * https://github.com/alexjlockwood/activity-transitions
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final String ARG_POSITION = "item_position";
    private static final String ARG_STARTING_POSITION = "item_starting_position";

    private Cursor mCursor;
    private long mItemId;
    private int mMutedColor = 0xFF333333;
    private View mRootView;

    @Bind(R.id.article_detail_toolbar)
    Toolbar mToolbar;
    @Bind(R.id.detail_photo)
    ImageView mPhotoView;
    @Bind(R.id.detail_article_title)
    TextView titleView;
    @Bind(R.id.detail_article_byline)
    TextView bylineView;
    @Bind(R.id.detail_article_body)
    TextView bodyView;
    @Bind(R.id.detail_meta_bar)
    LinearLayout mMetaDataLayout;

    @BindBool(R.bool.detail_is_card)
    boolean mIsCard = false;

    private int mStartingPosition;
    private int mArticlePosition;
    private MetaDataTarget mMetaDataTarget;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(ARG_POSITION, position);
        arguments.putInt(ARG_STARTING_POSITION, startingPosition);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
            mStartingPosition = getArguments().getInt(ARG_STARTING_POSITION);
            mArticlePosition = getArguments().getInt(ARG_POSITION);
            Log.d(TAG, "onCreate: " + mItemId);
        }
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);

        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        mToolbar.setTitle("");
        getActivityCast().setSupportActionBar(mToolbar);
        if (getActivityCast().getActionBar() != null) {
            getActivityCast().getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mToolbar.setNavigationIcon(ContextCompat.getDrawable(getActivity(), R.drawable.ic_arrow_back));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //onBackPressed()
                Log.d(TAG, "onClick: ");
                getActivityCast().supportFinishAfterTransition();
            }
        });
        return mRootView;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        bylineView.setMovementMethod(new LinkMovementMethod());

        if (mCursor != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mPhotoView.setTransitionName(String.valueOf(mArticlePosition));
            }

            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            String url = mCursor.getString(ArticleLoader.Query.PHOTO_URL);

            mMetaDataTarget = new MetaDataTarget();

            Picasso.with(getActivity())
                    .load(url)
                    .placeholder(R.drawable.empty_detail)
                    .into(mMetaDataTarget);

        } else {
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    public class MetaDataTarget implements Target {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            new Palette.Builder(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    Log.d(TAG, "onGenerated: ");
                    mMetaDataLayout.setBackgroundColor(palette.getDarkMutedColor(mMutedColor));
                }
            });
            mPhotoView.setImageBitmap(bitmap);
            startPostponedEnterTransition();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            startPostponedEnterTransition();
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            startPostponedEnterTransition();
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished: ");
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;

        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }


    @OnClick(R.id.share_fab)
    public void shareArticle() {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                .setType("text/plain")
                .setText("Some sample text")
                .getIntent(), getString(R.string.action_share)));
    }

    private void startPostponedEnterTransition() {
        if (mArticlePosition == mStartingPosition) {
            mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getActivity().startPostponedEnterTransition();
                    }
                    return true;
                }
            });
        }
    }

    @Nullable
    public ImageView getArticleImage() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }


}
