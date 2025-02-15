package com.androidessence.cashcaretaker.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;

import com.androidessence.cashcaretaker.DecimalDigitsInputFilter;
import com.androidessence.cashcaretaker.R;
import com.androidessence.cashcaretaker.alarms.RepeatingTransactionService;
import com.androidessence.cashcaretaker.data.CCContract;
import com.androidessence.cashcaretaker.dataTransferObjects.Account;
import com.androidessence.cashcaretaker.dataTransferObjects.Category;
import com.androidessence.cashcaretaker.dataTransferObjects.RepeatingPeriod;
import com.androidessence.cashcaretaker.dataTransferObjects.RepeatingTransaction;
import com.androidessence.utility.Utility;

import org.joda.time.LocalDate;

/**
 * Fragment for adding a repeating transaction.
 *
 * Created by adammcneilly on 11/16/15.
 */
public class AddRepeatingTransactionFragment extends Fragment implements RepeatingPeriodDialog.OnRepeatingPeriodSelectedListener, AccountDialog.OnAccountSelectedListener, CategoryDialog.OnCategorySelectedListener, DatePickerDialog.OnDateSetListener{
    private EditText mRepeatingPeriodEditText;
    private RepeatingPeriod mRepeatingPeriod;
    private EditText mAccountEditText;
    private Account mAccount;
    private EditText mDescription;
    private EditText mAmount;
    private EditText mNotes;
    private EditText mDateEditText;
    private LocalDate mDate;
    private EditText mCategoryEditText;
    private Category mCategory;
    private RadioButton mWithdrawal;
    private Button mSubmit;

    private static final String ARG_REPEATING_PERIOD = "argRepeatingPeriod";
    private static final String ARG_ACCOUNT = "argAccount";
    private static final String ARG_DATE = "argDate";
    private static final String ARG_CATEGORY = "argCategory";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_repeating_transaction, container, false);

        getUIElements(view);
        setTextFilters();
        setClickListeners();

        // Retrieve any saved values.
        if(savedInstanceState != null && savedInstanceState.containsKey(ARG_REPEATING_PERIOD)) {
            setRepeatingPeriod((RepeatingPeriod) savedInstanceState.getParcelable(ARG_REPEATING_PERIOD));
        } else {
            getDefaultRepeatingPeriod();
        }

        if(savedInstanceState != null && savedInstanceState.containsKey(ARG_ACCOUNT)) {
            setAccount((Account) savedInstanceState.getParcelable(ARG_ACCOUNT));
        }

        if(savedInstanceState != null && savedInstanceState.containsKey(ARG_DATE)) {
            setDate((LocalDate) savedInstanceState.getSerializable(ARG_DATE));
        } else {
            setDate(LocalDate.now());
        }

        if(savedInstanceState != null && savedInstanceState.containsKey(ARG_CATEGORY)) {
            setCategory((Category) savedInstanceState.getParcelable(ARG_CATEGORY));
        } else {
            getDefaultCategory();
        }

        return view;
    }

    /**
     * Retrieves all necessary UI elements for this fragment.
     */
    private void getUIElements(View view) {
        mRepeatingPeriodEditText = (EditText) view.findViewById(R.id.transaction_repeating_period);
        mAccountEditText = (EditText) view.findViewById(R.id.transaction_account);
        mDescription = (EditText) view.findViewById(R.id.transaction_description);
        mAmount = (EditText) view.findViewById(R.id.transaction_amount);
        mNotes = (EditText) view.findViewById(R.id.transaction_notes);
        mDateEditText = (EditText) view.findViewById(R.id.transaction_date);
        mCategoryEditText = (EditText) view.findViewById(R.id.transaction_category);
        mWithdrawal = (RadioButton) view.findViewById(R.id.transaction_withdrawal);
        mSubmit = (Button) view.findViewById(R.id.submit);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_REPEATING_PERIOD, mRepeatingPeriod);
        outState.putParcelable(ARG_ACCOUNT, mAccount);
        outState.putSerializable(ARG_DATE, mDate);
        outState.putParcelable(ARG_CATEGORY, mCategory);
    }

    /**
     * Sets input filters to necessary EditTexts.
     */
    private void setTextFilters() {
        InputFilter[] inputFilters = new InputFilter[] {new DecimalDigitsInputFilter()};
        mAmount.setFilters(inputFilters);
    }

    /**
     * Sets the appropriate click listeners for this fragment.
     */
    private void setClickListeners() {
        mRepeatingPeriodEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRepeatingPeriodDialog();
            }
        });
        mAccountEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountDialog();
            }
        });
        mCategoryEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCategoryDialog();
            }
        });
        mDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerFragment();
            }
        });
        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitRepeatingTransaction();
            }
        });
    }

    /**
     * Displays the dialog for selecting a repeating period.
     */
    private void showRepeatingPeriodDialog() {
        RepeatingPeriodDialog repeatingPeriodDialog = new RepeatingPeriodDialog();
        repeatingPeriodDialog.setTargetFragment(this, 0);
        repeatingPeriodDialog.show(getFragmentManager(), "repeatingPeriod");
    }

    /**
     * Displays the dialog for selecting an account.
     */
    private void showAccountDialog() {
        AccountDialog accountDialog = new AccountDialog();
        accountDialog.setTargetFragment(this, 0);
        accountDialog.show(getFragmentManager(), "account");
    }

    /**
     * Submits a repeating transaction entry to the database.
     */
    private void submitRepeatingTransaction() {
        if(!validateInput()) {
            return;
        }

        RepeatingTransaction repeatingTransaction = new RepeatingTransaction(
                mRepeatingPeriod.getIdentifier(),
                mAccount.getIdentifier(),
                mDescription.getText().toString(),
                Double.parseDouble(mAmount.getText().toString()),
                mNotes.getText().toString(),
                mDate,
                mCategory.getIdentifier(),
                mWithdrawal.isChecked()
        );

        // Submit row
        getActivity().getContentResolver().insert(CCContract.RepeatingTransactionEntry.CONTENT_URI, repeatingTransaction.getContentValues());

        // Start service
        getActivity().startService(new Intent(getActivity(), RepeatingTransactionService.class));

        // Finish activity.
        getActivity().finish();
    }

    /**
     * Validates all required input.
     * @return True if the input in the fragment is valid, false otherwise.
     */
    private boolean validateInput() {
        boolean isValid = true;

        // Do not check RepeatingPeriod, Category they are both set by default.
        if(mAccount == null) {
            mAccountEditText.setError("Account must be selected.");
            isValid = false;
        }

        if(mDescription.getText().toString().isEmpty()) {
            mDescription.setError("Description cannot be blank.");
            isValid = false;
        }

        if(mAmount.getText().toString().isEmpty()) {
            mAmount.setError("Amount cannot be blank.");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Retrieves the default category to be used.
     */
    private void getDefaultCategory(){
        //TODO: Don't hard code this, put it in settings or something
        String defaultCategory = "None";

        Cursor cursor = getActivity().getContentResolver().query(
                CCContract.CategoryEntry.CONTENT_URI,
                null,
                CCContract.CategoryEntry.COLUMN_DESCRIPTION + " = ?",
                new String[] {defaultCategory},
                null
        );

        assert cursor != null;
        if(cursor.moveToFirst()){
            setCategory(new Category(cursor));
        } else{
            // Bad input, just set empty for now.
            setCategory(new Category());
        }

        cursor.close();
    }

    /**
     * Gets the default repeating period, which is just the first entry.
     */
    private void getDefaultRepeatingPeriod() {
        Cursor cursor = getActivity().getContentResolver().query(
                CCContract.RepeatingPeriodEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        assert cursor != null;

        if(cursor.moveToFirst()) {
            setRepeatingPeriod(new RepeatingPeriod(cursor));
        } else {
            // Bad input, set empty.
            setRepeatingPeriod(new RepeatingPeriod());
        }

        cursor.close();
    }

    private void setDate(LocalDate date) {
        mDate = date;
        mDateEditText.setText(Utility.getUIDateString(mDate));
    }

    /**
     * Sets the repeating period for the transaction.
     */
    private void setRepeatingPeriod(RepeatingPeriod repeatingPeriod) {
        mRepeatingPeriod = repeatingPeriod;
        mRepeatingPeriodEditText.setText(repeatingPeriod.getName());
    }

    /**
     * Sets the transaction category.
     */
    private void setCategory(Category category){
        this.mCategory = category;
        this.mCategoryEditText.setText(mCategory.getDescription());
    }

    /**
     * Sets the account for the transaction.
     */
    private void setAccount(Account account) {
        mAccount = account;
        mAccountEditText.setText(mAccount.getName());
        // Remove error if it was there
        mAccountEditText.setError(null);
    }

    @Override
    public void onRepeatingPeriodSelected(RepeatingPeriod repeatingPeriod) {
        setRepeatingPeriod(repeatingPeriod);
    }

    @Override
    public void onAccountSelected(Account account) {
        setAccount(account);
    }

    /**
     * Displays the category dialog.
     */
    private void showCategoryDialog(){
        CategoryDialog dialog = new CategoryDialog();
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), "transactionCategory");
    }

    @Override
    public void onCategorySelected(Category category) {
        setCategory(category);
    }

    /**
     * Handles the selection of a date in the DatePickerDialog.
     */
    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        setDate(new LocalDate(year, monthOfYear + 1, dayOfMonth));
    }

    /**
     * Displays a date picker dialog.
     */
    private void showDatePickerFragment(){
        DatePickerFragment datePickerFragment = DatePickerFragment.NewInstance(mDate);
        datePickerFragment.setTargetFragment(this, 0);
        datePickerFragment.show(getFragmentManager(), "transactionDate");
    }
}
