
package org.wordpress.android.ui.accounts;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.util.AlertUtil;
import org.wordpress.android.util.UserEmail;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.emailchecker.EmailChecker;
import org.xmlpull.v1.XmlPullParser;

import java.util.Hashtable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewUserPageFragment extends NewAccountAbstractPageFragment implements TextWatcher {
    private EditText mSiteUrlTextField;
    private EditText mEmailTextField;
    private EditText mPasswordTextField;
    private EditText mUsernameTextField;
    private WPTextView mSignupButton;
    private EmailChecker mEmailChecker;
    private boolean mEmailAutoCorrected;

    public NewUserPageFragment() {
        mEmailChecker = new EmailChecker();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (fieldsFilled()) {
            mSignupButton.setEnabled(true);
        } else {
            mSignupButton.setEnabled(false);
        }
    }

    private boolean fieldsFilled() {
        return mEmailTextField.getText().toString().trim().length() > 0
                && mPasswordTextField.getText().toString().trim().length() > 0
                && mUsernameTextField.getText().toString().trim().length() > 0
                && mSiteUrlTextField.getText().toString().trim().length() > 0;
    }

    private boolean checkUserData() {
        // try to create the user
        final String email = mEmailTextField.getText().toString().trim();
        final String password = mPasswordTextField.getText().toString().trim();
        final String username = mUsernameTextField.getText().toString().trim();

        if (email.equals("")) {
            mEmailTextField.setError(getString(R.string.required_field));
            mEmailTextField.requestFocus();
            return false;
        }

        final String emailRegEx = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
        final Pattern emailRegExPattern = Pattern.compile(emailRegEx,
                Pattern.DOTALL);
        Matcher matcher = emailRegExPattern.matcher(email);
        if (!matcher.find() || email.length() > 100) {
            mEmailTextField.setError(getString(R.string.invalid_email_message));
            mEmailTextField.requestFocus();
            return false;
        }

        if (username.equals("")) {
            mUsernameTextField.setError(getString(R.string.required_field));
            mUsernameTextField.requestFocus();
            return false;
        }

        if (username.length() < 4) {
            mUsernameTextField.setError(getString(R.string.invalid_username_too_short));
            mUsernameTextField.requestFocus();
            return false;
        }

        if (username.length() > 60) {
            mUsernameTextField.setError(getString(R.string.invalid_username_too_long));
            mUsernameTextField.requestFocus();
            return false;
        }

        if (password.equals("")) {
            mPasswordTextField.setError(getString(R.string.required_field));
            mPasswordTextField.requestFocus();
            return false;
        }

        if (password.length() < 4) {
            mPasswordTextField.setError(getString(R.string.invalid_password_message));
            mPasswordTextField.requestFocus();
            return false;
        }

        return true;
    }

    OnClickListener signupClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            validateAndCreateUserAndBlog();
        }
    };

    private String siteUrlToSiteName(String siteUrl) {
        return siteUrl;
    }

    private void finishThisStuff(String username) {
        final NewAccountActivity act = (NewAccountActivity) getActivity();
        Bundle bundle = new Bundle();
        bundle.putString("username", username);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        act.setResult(act.RESULT_OK, intent);
        act.finish();
    }

    private void validateAndCreateUserAndBlog() {
        if (mSystemService.getActiveNetworkInfo() == null) {
            AlertUtil.showAlert(getActivity(), R.string.no_network_title,
                    R.string.no_network_message);
            return;
        }
        if (!checkUserData())
            return;

        final String siteUrl = mSiteUrlTextField.getText().toString().trim();
        final String email = mEmailTextField.getText().toString().trim();
        final String password = mPasswordTextField.getText().toString().trim();
        final String username = mUsernameTextField.getText().toString().trim();
        final String siteName = siteUrlToSiteName(siteUrl);
        final String language = getDeviceLanguage();

        mProgressDialog = ProgressDialog.show(getActivity(),
                getString(R.string.account_setup), getString(R.string.validating_user_data),
                true, false);

        CreateUserAndBlog createUserAndBlog = new CreateUserAndBlog(email, username, password,
                siteUrl, siteName, language, restClient, getActivity(), new ErrorListener(),
                new CreateUserAndBlog.Callback() {
                    @Override
                    public void onStepFinished(CreateUserAndBlog.Step step) {
                        switch (step) {
                            case VALIDATE_USER:
                                mProgressDialog.setMessage(getString(R.string.validating_site_data));
                                break;
                            case VALIDATE_SITE:
                                mProgressDialog.setMessage(getString(R.string.create_account_wpcom));
                                break;
                            case CREATE_USER:
                                mProgressDialog.setMessage(getString(R.string.create_blog_wpcom));
                                break;
                            case CREATE_SITE: // no messages
                            case AUTHENTICATE_USER:
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onSuccess() {
                        mProgressDialog.dismiss();
                        finishThisStuff(username);
                    }

                    @Override
                    public void onError(int messageId) {
                        mProgressDialog.dismiss();
                        showError(getString(messageId));
                    }
                });
        createUserAndBlog.startCreateUserAndBlogProcess();
    }

    private void autocorrectEmail() {
        if (mEmailAutoCorrected)
            return;
        final String email = mEmailTextField.getText().toString().trim();
        String suggest = mEmailChecker.suggestDomainCorrection(email);
        if (suggest.compareTo(email) != 0) {
            mEmailAutoCorrected = true;
            mEmailTextField.setText(suggest);
            mEmailTextField.setSelection(suggest.length());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.new_account_user_fragment_screen, container, false);

        WPTextView termsOfServiceTextView = (WPTextView) rootView.findViewById(R.id.l_agree_terms_of_service);
        termsOfServiceTextView.setText(Html.fromHtml(String.format(getString(R.string.agree_terms_of_service, "<u>", "</u>"))));
        termsOfServiceTextView.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri uri = Uri.parse(Constants.URL_TOS);
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    }
                }
        );

        mSignupButton = (WPTextView) rootView.findViewById(R.id.signup_button);
        mSignupButton.setOnClickListener(signupClickListener);
        mSignupButton.setEnabled(false);

        mEmailTextField = (EditText) rootView.findViewById(R.id.email_address);
        mEmailTextField.setText(UserEmail.getPrimaryEmail(getActivity()));
        mEmailTextField.setSelection(mEmailTextField.getText().toString().length());
        mPasswordTextField = (EditText) rootView.findViewById(R.id.password);
        mUsernameTextField = (EditText) rootView.findViewById(R.id.username);
        mSiteUrlTextField = (EditText) rootView.findViewById(R.id.site_url);

        mEmailTextField.addTextChangedListener(this);
        mPasswordTextField.addTextChangedListener(this);
        mUsernameTextField.addTextChangedListener(this);
        mSiteUrlTextField.addTextChangedListener(this);
        mUsernameTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // auto fill blog address
                mSiteUrlTextField.setText(mUsernameTextField.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mEmailTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    autocorrectEmail();
                }
            }
        });

        return rootView;
    }

    private String getDeviceLanguage() {
        Resources res = getActivity().getResources();
        XmlResourceParser parser = res.getXml(R.xml.wpcom_languages);
        Hashtable<String, String> entries = new Hashtable<String, String>();
        String matchedDeviceLanguage = "en - English";
        try {
            int eventType = parser.getEventType();
            String deviceLanguageCode = Locale.getDefault().getLanguage();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("language")) {
                        String currentID = null;
                        boolean currentLangIsDeviceLanguage = false;
                        int i = 0;
                        while (i < parser.getAttributeCount()) {
                            if (parser.getAttributeName(i).equals("id")) {
                                currentID = parser.getAttributeValue(i);
                            }
                            if (parser.getAttributeName(i).equals("code") && parser.
                                    getAttributeValue(i).equalsIgnoreCase(deviceLanguageCode)) {
                                currentLangIsDeviceLanguage = true;
                            }
                            i++;
                        }

                        while (eventType != XmlPullParser.END_TAG) {
                            if (eventType == XmlPullParser.TEXT) {
                                entries.put(parser.getText(), currentID);
                                if (currentLangIsDeviceLanguage) {
                                    matchedDeviceLanguage = parser.getText();
                                }
                            }
                            eventType = parser.next();
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            // do nothing
        }
        return matchedDeviceLanguage;
    }
}