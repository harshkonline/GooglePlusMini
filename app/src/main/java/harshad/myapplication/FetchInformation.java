package harshad.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.plusDomains.PlusDomains;
import com.google.api.services.plusDomains.model.Circle;
import com.google.api.services.plusDomains.model.CircleFeed;
import com.google.api.services.plusDomains.model.PeopleFeed;
import com.google.api.services.plusDomains.model.Person;

import java.io.IOException;
import java.util.List;

public class FetchInformation extends AsyncTask<String, Void, Boolean> {

    private ProgressDialog dialog;

    LoginActivity mActivity;
    private Context context;
    String mScope;
    String mEmail;
    String token;


    Person me;
    private static String displayName;
    private static String occupation;
    private static String organizations;
    private static String aboutMe;
    private static String image_url;


    String[] circle_list;
    static String[][] circle_children;
    static Person[][] circle_children_people;
    PlusDomains.Circles.List listCircles;
    CircleFeed circleFeed;
    List<Circle> circles;
    PlusDomains.People.ListByCircle listPeople;

    FetchInformation(LoginActivity activity, String name, String scope) {
        this.mActivity = activity;
        context = activity;
        this.mScope = scope;
        this.mEmail = name;
    }


    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        dialog.setMessage("Welcome to my app");
        dialog.show();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            token = fetchToken();
            if (token != null) {
                GoogleCredential cred = new GoogleCredential().setAccessToken(token);
                PlusDomains plusDomains = new PlusDomains.Builder(new NetHttpTransport(), new JacksonFactory(), cred).build();

                me = plusDomains.people().get("me").execute();

                displayName = me.getDisplayName();
                aboutMe = me.getAboutMe();
                image_url = me.getImage().getUrl();
                occupation = me.getOccupation();

                organizations = "";
                if(me.getOrganizations() != null) {
                    List<Person.Organizations> tmp = me.getOrganizations();
                    for (Person.Organizations o: tmp){
                        organizations = organizations + " " + o.getName() + ",";
                    }
                    organizations = organizations.substring(0, organizations.length()-1); //remove the last comma
                }


                listCircles = plusDomains.circles().list("me");
                if(listCircles != null) {
                    circleFeed = listCircles.execute();
                    circles = circleFeed.getItems();
                    circle_list = new String[circles.size()];
                    circle_children = new String[circles.size()][];
                    circle_children_people = new Person[circles.size()][];

                    while (circles != null) {
                        int i = 0;
                        for (Circle circle : circles) {
                            String name = circle.getDisplayName();
                            circle_list[i] = name;

                            String id = circle.getId();
                            listPeople = plusDomains.people().listByCircle(id);
                            PeopleFeed peopleFeed = listPeople.execute();

                            if(peopleFeed.getItems() != null && peopleFeed.getItems().size() > 0 ) {
                                circle_children[i] = new String[peopleFeed.getItems().size()];
                                circle_children_people[i] = new Person[peopleFeed.getItems().size()];
                                int j = 0;
                                for(Person person : peopleFeed.getItems()) {
                                    circle_children[i][j] = person.getDisplayName();
                                    circle_children_people[i][j] = person;

                                    j++;
                                }
                            }
                            else {
                                circle_children[i] = new String[0];
                                circle_children_people[i] = new Person[0];
                            }
                            i++;
                        }


                        if (circleFeed.getNextPageToken() != null) {

                            listCircles.setPageToken(circleFeed.getNextPageToken());


                            circleFeed = listCircles.execute();
                            circles = circleFeed.getItems();
                        } else {
                            circles = null;
                        }
                    }
                }

            }
        } catch (IOException e) {
            Log.d("", "exception", e);
        }
        return true;
    }


    @Override
    protected void onPostExecute(final Boolean success) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }

        Intent activity = new Intent(context, GooglePlusActivity.class);
        activity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.putExtra("accessToken", token);


        activity.putExtra("displayName", displayName);
        activity.putExtra("organizations", organizations);
        activity.putExtra("aboutMe", aboutMe);
        activity.putExtra("image_url", image_url);
        activity.putExtra("occupation", occupation);


        if(circle_list.length > 0) {
            activity.putExtra("circle_view", circle_list);
        }
        context.startActivity(activity);
    }


    public static String[][] getArray() {
        return circle_children;
    }


    public static Person[][] getCircle_children_people(){
        return circle_children_people;
    }

    protected String fetchToken() throws IOException {
        try {
            return GoogleAuthUtil.getToken(mActivity, mEmail, mScope);
        } catch (UserRecoverableAuthException userRecoverableException) {

            mActivity.handleException(userRecoverableException);
        } catch (GoogleAuthException fatalException) {

            fatalException.printStackTrace();
        }
        return null;
    }

}