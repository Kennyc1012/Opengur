package com.kenny.openimgur.ui;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by Kenny-PC on 6/20/2015.
 */
public class GridItemDecoration extends RecyclerView.ItemDecoration {
    private int mSpace;

    private int mNumColumns;

    public GridItemDecoration(int spacing, int numColumns) {
        mSpace = spacing;
        mNumColumns = numColumns;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getLayoutManager().getPosition(view);
        int mod = position % mNumColumns;

        switch (mNumColumns) {
            case 3:
                if (mod == 1) {
                    outRect.left = mSpace;
                    outRect.right = mSpace;
                }
                break;

            case 4:
                if (mod == 1) {
                    outRect.left = mSpace;
                } else if (mod == 2) {
                    outRect.left = mSpace;
                    outRect.right = mSpace;
                }
                break;

            case 5:
                if (mod == 1 || mod == 2) {
                    outRect.left = mSpace;
                } else if (mod == 3) {
                    outRect.left = mSpace;
                    outRect.right = mSpace;
                }
                break;
        }

        outRect.top = mSpace;
    }
}