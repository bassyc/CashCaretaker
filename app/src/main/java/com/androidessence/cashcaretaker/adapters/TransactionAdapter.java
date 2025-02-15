package com.androidessence.cashcaretaker.adapters;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androidessence.cashcaretaker.R;
import com.androidessence.cashcaretaker.data.CCContract;
import com.androidessence.cashcaretaker.dataTransferObjects.TransactionDetails;
import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorAdapter;
import com.androidessence.recyclerviewcursoradapter.RecyclerViewCursorViewHolder;
import com.androidessence.utility.Utility;

/**
 * An apter for displaying a list of Transaction objects.
 *
 * Created by adammcneilly on 11/1/15.
 */
public class TransactionAdapter extends RecyclerViewCursorAdapter<TransactionAdapter.TransactionViewHolder> {
    /**
     * A list of data columns required for creating and displaying a Transaction object.
     */
    public static final String[] TRANSACTION_COLUMNS = new String[] {
            CCContract.TransactionEntry.TABLE_NAME + "." + CCContract.TransactionEntry._ID,
            CCContract.TransactionEntry.COLUMN_DESCRIPTION,
            CCContract.TransactionEntry.COLUMN_AMOUNT,
            CCContract.TransactionEntry.COLUMN_WITHDRAWAL,
            CCContract.TransactionEntry.COLUMN_NOTES,
            CCContract.TransactionEntry.COLUMN_DATE,
            CCContract.CategoryEntry.COLUMN_DESCRIPTION,
            CCContract.TransactionEntry.COLUMN_CATEGORY,
            CCContract.TransactionEntry.COLUMN_ACCOUNT
    };

    // Indexes for each of the columns of display data.
    private static final int DESCRIPTION_INDEX = 1;
    private static final int AMOUNT_INDEX = 2;
    private static final int WITHDRAWAL_INDEX = 3;
    private static final int NOTES_INDEX = 4;
    private static final int DATE_INDEX = 5;
    private static final int CATEGORY_INDEX = 6;

    // Colors used inside the ViewHolder.
    private final int mRed;
    private final int mGreen;
    private final int mPrimaryText;

    public TransactionAdapter(Context context){
        super(context);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mRed = mContext.getColor(R.color.mds_red_500);
            mGreen = mContext.getColor(R.color.mds_green_500);
            mPrimaryText = mContext.getColor(android.R.color.primary_text_light);
        } else {
            mRed = mContext.getResources().getColor(R.color.mds_red_500);
            mGreen = mContext.getResources().getColor(R.color.mds_green_500);
            mPrimaryText = mContext.getResources().getColor(android.R.color.primary_text_light);
        }

        setupCursorAdapter(null, 0, R.layout.list_item_transaction, false);
    }

    @Override
    public TransactionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TransactionViewHolder(mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent));
    }

    @Override
    public void onBindViewHolder(TransactionViewHolder holder, int position) {
        setViewHolder(holder);

        // Move cursor to the current position
        mCursorAdapter.getCursor().moveToPosition(position);
        mCursorAdapter.bindView(null, mContext, mCursorAdapter.getCursor());
    }

    public class TransactionViewHolder extends RecyclerViewCursorViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public final TextView mDescriptionTextView;
        public final TextView mAmountTextView;
        public final TextView mDateTextView;
        public final TextView mCategoryTextView;
        public final TextView mNotesTextView;
        public final View mIndicatorView;

        public TransactionViewHolder(View view){
            super(view);
            mDescriptionTextView = (TextView) view.findViewById(R.id.transaction_description);
            mAmountTextView = (TextView) view.findViewById(R.id.transaction_amount);
            mDateTextView = (TextView) view.findViewById(R.id.transaction_date);
            mCategoryTextView = (TextView) view.findViewById(R.id.transaction_category);
            mNotesTextView = (TextView) view.findViewById(R.id.transaction_notes);
            mIndicatorView = view.findViewById(R.id.transaction_indicator);

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        public void bindCursor(Cursor cursor) {
            // Set description
            mDescriptionTextView.setText(cursor.getString(DESCRIPTION_INDEX));

            // Set amount
            double amount = cursor.getDouble(AMOUNT_INDEX);
            mAmountTextView.setText(Utility.getCurrencyString(amount));

            // Set withdrawal. Depending on withdrawal, we need to color certain views.
            int isWithdrawal = cursor.getInt(WITHDRAWAL_INDEX);
            if(isWithdrawal == 1) {
                mAmountTextView.setText(String.format("-%s", Utility.getCurrencyString(amount)));
                mAmountTextView.setTextColor(mRed);
                mIndicatorView.setBackgroundColor(mRed);
            } else{
                mAmountTextView.setText(Utility.getCurrencyString(amount));
                mAmountTextView.setTextColor(mPrimaryText);
                mIndicatorView.setBackgroundColor(mGreen);
            }

            // Set date
            String dateString = cursor.getString(DATE_INDEX);
            mDateTextView.setText(Utility.getUIDateStringFromDB(dateString));

            //TODO: Globals somewhere
            String defaultCategory = "None";
            String category = cursor.getString(CATEGORY_INDEX);
            if(category.equals(defaultCategory)) {
                mCategoryTextView.setText("");
            } else{
                mCategoryTextView.setText(category);
            }

            // Set notes.
            //TODO: Use resource
            mNotesTextView.setText(String.format("Notes: %s", cursor.getString(NOTES_INDEX)));
        }

        @Override
        public void onClick(View v) {
            // Toggle notes
            if(mNotesTextView.getVisibility() == View.VISIBLE) {
                mNotesTextView.setVisibility(View.GONE);
            } else if(mNotesTextView.getVisibility() == View.GONE) {
                mNotesTextView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            // Get current item and call back to activity
            mCursorAdapter.getCursor().moveToPosition(getAdapterPosition());
            ((OnTransactionLongClickListener)mContext).onTransactionLongClick(new TransactionDetails(mCursorAdapter.getCursor()));
            return true;
        }
    }

    public interface OnTransactionLongClickListener {
        void onTransactionLongClick(TransactionDetails transaction);
    }
}
