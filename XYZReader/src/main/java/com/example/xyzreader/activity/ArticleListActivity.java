package com.example.xyzreader.activity;

import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import com.example.xyzreader.R;
import com.example.xyzreader.adapter.ArticleAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.ui.SpacesItemDecoration;

import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.BindDimen;
import butterknife.BindInt;
import butterknife.ButterKnife;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 * <p>
 * Credits to Alex Lockwood for a shared element transition example in fragments
 * https://github.com/alexjlockwood/activity-transitions
 */

public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        ArticleAdapter.AdapterItemListener {


    public static final String EXTRA_STARTING_ALBUM_POSITION = "extra_starting_item_position";
    public static final String EXTRA_CURRENT_ALBUM_POSITION = "extra_current_item_position";


    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.main_coordinator)
    CoordinatorLayout mCoordinatorLayout;
    @Bind(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @BindInt(R.integer.list_column_count)
    int mColumnCount;
    @BindDimen(R.dimen.spacing_normal)
    int mSpaceItems;

    private ArticleAdapter mAdapter;
    private boolean mIsRefreshing = false;
    private Bundle mTmpReenterState;
    private boolean mIsDetailsActivityStarted;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SharedElementCallback mCallback = new SharedElementCallback() {
                @Override
                public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                    if (mTmpReenterState != null) {
                        int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ALBUM_POSITION);
                        int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ALBUM_POSITION);
                        if (startingPosition != currentPosition) {
                            // If startingPosition != currentPosition the user must have swiped to a
                            // different page in the DetailsActivity. We must update the shared element
                            // so that the correct one falls into place.
                            String newTransitionName = String.valueOf(currentPosition);
                            View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
                            if (newSharedElement != null) {
                                names.clear();
                                names.add(newTransitionName);
                                sharedElements.clear();
                                sharedElements.put(newTransitionName, newSharedElement);
                            }
                        }

                        mTmpReenterState = null;
                    } else {
                        // If mTmpReenterState is null, then the activity is exiting.
                        View navigationBar = findViewById(android.R.id.navigationBarBackground);
                        View statusBar = findViewById(android.R.id.statusBarBackground);
                        if (navigationBar != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                names.add(navigationBar.getTransitionName());
                                sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                            }
                        }
                        if (statusBar != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                names.add(statusBar.getTransitionName());
                                sharedElements.put(statusBar.getTransitionName(), statusBar);
                            }
                        }
                    }
                }
            };
            setExitSharedElementCallback(mCallback);
        }

        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(mColumnCount, StaggeredGridLayoutManager.VERTICAL);
        SpacesItemDecoration decoration = new SpacesItemDecoration(mSpaceItems);

        mRecyclerView.setLayoutManager(sglm);

        mAdapter = new ArticleAdapter(this, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(decoration);

        getLoaderManager().initLoader(0, null, this);

        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.progress_colors));
        if (savedInstanceState == null) {
            onRefresh();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mTmpReenterState = new Bundle(data.getExtras());
        int startingPosition = mTmpReenterState.getInt(EXTRA_STARTING_ALBUM_POSITION);
        int currentPosition = mTmpReenterState.getInt(EXTRA_CURRENT_ALBUM_POSITION);
        if (startingPosition != currentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
        }
        postponeEnterTransition();
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                // TODO: figure out why it is necessary to request layout here in order to get a smooth transition.
                mRecyclerView.requestLayout();
                startPostponedEnterTransition();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsDetailsActivityStarted = false;
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.refresh) {
            Snackbar.make(mCoordinatorLayout, "Refresh list",
                    Snackbar.LENGTH_SHORT).show();
            onRefresh();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onRefresh() {
        refresh();
    }

    @Override
    public void onItemClick(Uri uri, int position, View view) {
        ActivityOptionsCompat options = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            options = ActivityOptionsCompat.
                    makeSceneTransitionAnimation(this, view, view.getTransitionName());
        }

        if (!mIsDetailsActivityStarted) {
            mIsDetailsActivityStarted = true;
            Intent intent = new Intent(this, ArticleDetailActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(uri);
            ActivityCompat.startActivity(this, intent, options == null ? null : options.toBundle());
        }
    }
}
