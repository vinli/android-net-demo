package li.vin.netdemo;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import li.vin.net.Coordinate;
import li.vin.net.Device;
import li.vin.net.Location;
import li.vin.net.Page;
import li.vin.net.StreamMessage;
import li.vin.net.User;
import li.vin.net.Vehicle;
import li.vin.net.Vinli;
import li.vin.net.VinliApp;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Connect to the Vinli platform, log some basic info in a bunch of TextViews.
 * <br><br>
 * Uses <a href="https://github.com/vinli/android-net">the Vinli Net SDK</a>, <a
 * href="https://github.com/ReactiveX/RxJava">RxJava</a>, and
 * <a href="http://jakewharton.github.io/butterknife/">Butter Knife</a>.
 */
public class NetDemoActivity extends AppCompatActivity {

  private final int DEFAULT_VALUE = -10101;
  private final String TAG = this.getClass().getSimpleName();

  private TextView firstName;
  private TextView lastName;
  private TextView email;
  private TextView phone;
  private LinearLayout deviceContainer;

  private boolean contentBound;
  private boolean signInRequested;
  private VinliApp vinliApp;
  private CompositeSubscription subscription;
  private Subscription streamSubscription;

  @Override
  protected void onResume() {
    super.onResume();

    loadApp(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    loadApp(intent);
  }

  @Override
  protected void onPause(){
    super.onPause();

    // We don't want to continue the stream in the background.
    stopStream();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    cleanupSubscription();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu){
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    int id = item.getItemId();

    if(id == R.id.action_sign_out){
      onSignOutClick();
      return true;
    }else{
      return super.onOptionsItemSelected(item);
    }
  }

  void onSignOutClick() {
    if (vinliApp != null && !isFinishing()) {
      stopStream();
      signIn();
    }
  }

  /**
   * Load the VinliApp instance if possible - if not, proceed by either signing in or finishing the
   * Activity.
   */
  private void loadApp(Intent intent) {
    if (vinliApp == null) {
      vinliApp = (intent == null)
          ? Vinli.loadApp(this)
          : Vinli.initApp(this, intent);
      if (vinliApp == null) {
        if (signInRequested) {
          // If a sign in was already requested, it failed or was canceled - finish.
          finish();
        } else {
          // Otherwise, sign in.
          signIn();
        }
      } else {
        // Succesfully loaded VinliApp - proceed.
        setupContent();
        subscribeAll();
      }
    }
  }

  /** Clear existing session state and sign in. */
  private void signIn() {
    signInRequested = true;
    setIntent(new Intent());
    Vinli.clearApp(this);
    vinliApp = null;
    killAllCookies();
    Vinli.signIn(this,
        getString(R.string.app_client_id), // Get your app id from dev.vin.li
        getString(R.string.app_redirect_uri), // Set your app redirect uri at dev.vin.li
        PendingIntent.getActivity(this, 0, new Intent(this, NetDemoActivity.class), 0));
  }

  /** Set content view and bind views. Only do this once. */
  private void setupContent() {
    if (contentBound) return;
    setContentView(R.layout.activity_main);

    firstName = (TextView) findViewById(R.id.first_name);
    lastName = (TextView) findViewById(R.id.last_name);
    email = (TextView) findViewById(R.id.email);
    phone = (TextView) findViewById(R.id.phone);
    deviceContainer = (LinearLayout) findViewById(R.id.device_container);

    contentBound = true;
  }

  /** Permissively subscribe to all data. Clean up beforehand if necessary. */
  private void subscribeAll() {
    // Clean up an existing subscriptions that are ongoing.
    cleanupSubscription();

    // Sanity check.
    if (vinliApp == null || !contentBound) return;

    // Generic composite subscription to hold all individual subscriptions to data.
    subscription = new CompositeSubscription();

    // Remove all views from device container - best practice would be to use an AdapterView for
    // this, such as ListView or RecyclerView, but for this simple example it's less verbose to
    // do it this way.
    deviceContainer.removeAllViews();

    subscription.add(vinliApp.currentUser() // Fetch the current signed in user
        .observeOn(AndroidSchedulers.mainThread()) // Call onCompleted/onError/onNext on the main/UI thread
        .subscribe(new Subscriber<User>() {
      @Override
      public void onCompleted() {

      }

      @Override
      public void onError(Throwable e) {
        Log.e(TAG, "Error fetching user: " + e.getMessage());
      }

      @Override
      public void onNext(User user) {
        setStyledText(getString(R.string.first_name), user.firstName(), firstName);
        setStyledText(getString(R.string.last_name), user.lastName(), lastName);
        setStyledText(getString(R.string.email), user.email(), email);
        setStyledText(getString(R.string.phone), user.phone(), phone);
      }
    }));

    // Loop through each of the user's devices...
    subscription.add(vinliApp.devices()
        .flatMap(Page.<Device>allItems()) // Get all devices from all pages
        .observeOn(AndroidSchedulers.mainThread()) // Run the onCompleted/onError/onNext on Android's main/UI thread
        .subscribe(new Subscriber<Device>() {
          @Override
          public void onCompleted() { // Called after we have finished fetching all devices.
          }

          @Override
          public void onError(Throwable e) { // Called if something goes wrong fetching devices.
            Log.e(TAG, "Error fetching devices: " + e.getMessage());
          }

          @Override
          public void onNext(Device device) { // Called for each device that we fetch.
            // Inflate device layout into device container. See above note about how using an
            // AdapterView would be better if this weren't just a naive example.
            View v = LayoutInflater.from(NetDemoActivity.this).inflate(R.layout.device_layout, deviceContainer, false);
            TextView deviceName = (TextView) v.findViewById(R.id.device_name);
            final TextView latestVehicle = (TextView) v.findViewById(R.id.latest_vehicle);
            final TextView latestLocation = (TextView) v.findViewById(R.id.latest_location);
            Button streamButton = (Button) v.findViewById(R.id.stream_button);
            deviceContainer.addView(v);

            streamButton.setTag(device);

            streamButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                NetDemoActivity.this.streamButtonPressed((Button) v);
              }
            });

            setStyledText(getString(R.string.device_name), (device.name() != null) ? device.name() : getString(R.string.unnamed_device), deviceName);

            subscription.add(device.latestVehicle() // Get the latest vehicle for this device
                .observeOn(AndroidSchedulers.mainThread()) // Run the onCompleted/onError/onNext on Android's main/UI thread
                .subscribe(new Subscriber<Vehicle>() {
                  @Override
                  public void onCompleted() {

                  }

                  @Override
                  public void onError(Throwable e) {
                    Log.e(TAG, "Error fetching latest vehicle: " + e.getMessage());
                  }

                  @Override
                  public void onNext(Vehicle vehicle) {
                    String vehicleStr = (vehicle != null) ? vehicle.vin() : getString(R.string.none);
                    setStyledText(getString(R.string.latest_vehicle), vehicleStr, latestVehicle);
                  }
                }));

            subscription.add(device.latestlocation() // Get the latest location for this device.
                .observeOn(AndroidSchedulers.mainThread()) // Run the onCompleted/onError/onNext on Android's main/UI thread.
                .subscribe(new Subscriber<Location>() {
              @Override
              public void onCompleted() {

              }

              @Override
              public void onError(Throwable e) {
                Log.e(TAG, "Error fetching latest location: " + e.getMessage());
              }

              @Override
              public void onNext(Location location) {
                String locStr = (location != null) ? (location.coordinate().lat() + ", " + location.coordinate().lon()) : getString(R.string.none);
                setStyledText(getString(R.string.latest_location), locStr, latestLocation);
              }
            }));
          }
        }));
  }

  /** Unsubscribe all. Need to call this to clean up rx resources. */
  private void cleanupSubscription() {
    if (subscription != null) {
      if (!subscription.isUnsubscribed()) subscription.unsubscribe();
      subscription = null;
    }

    if(streamSubscription != null){
      if(!streamSubscription.isUnsubscribed()) streamSubscription.unsubscribe();
      streamSubscription = null;
    }
  }

  /**
   * Kill all WebView cookies. Need this to sign out & in properly, so WebView doesn't cache the
   * last session.
   */
  @SuppressWarnings("deprecation")
  private void killAllCookies() {
    CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.removeAllCookie();
    if (Build.VERSION.SDK_INT >= 21) cookieManager.removeAllCookies(null);
    cookieSyncManager.sync();
    if (Build.VERSION.SDK_INT >= 21) cookieManager.flush();
  }

  private void setStyledText(String label, String value, TextView textView){
    textView.setText(Html.fromHtml(String.format(getString(R.string.success_fmt), label, value)));
  }

  private void streamButtonPressed(Button button){
    Device device = (Device) button.getTag();

    // Lets only be streaming one device at a time, so kill the other stream off if it already exists.
    stopStream();

    // Start the stream from the device object
    Log.i(TAG, "Starting stream for device: " + device.id());
    streamSubscription = device.stream() // Create the stream for this device.
        .observeOn(AndroidSchedulers.mainThread()) // Call onCompleted/onError/onNext on Android's main/UI thread.
        .subscribe(new Subscriber<StreamMessage>() { // To stop the stream, call streamSubscription.unsubscribe();
          @Override
          public void onCompleted() {

          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "Error getting messages from stream: " + e.getMessage());
          }

          @Override
          public void onNext(StreamMessage streamMessage) {
            // Grab the RPM value from the StreamMessage, if RPM is not in the message it return DEFAULT_VALUE
            int rpm = streamMessage.intVal(StreamMessage.DataType.RPM, DEFAULT_VALUE);
            if (rpm != DEFAULT_VALUE) {
              Log.i(TAG, "Rpm: " + rpm);
            }

            // Get the current location of the device from the stream. It defaults to null if its not in the message.
            Coordinate coord = streamMessage.coord();
            if (coord != null) {
              Log.i(TAG, String.format("Latitude: %f Longitude %f", coord.lat(), coord.lon()));
            }
          }
        });
  }

  private void stopStream(){
    // Unsubscribe to stop the stream.
    if(streamSubscription != null && !streamSubscription.isUnsubscribed()){
      streamSubscription.unsubscribe();
    }
  }
}
