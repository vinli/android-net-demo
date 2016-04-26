package li.vin.netdemo;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.pkmmte.view.CircularImageView;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import li.vin.net.Device;
import li.vin.net.DistanceUnit;
import li.vin.net.Location;
import li.vin.net.Odometer;
import li.vin.net.Page;
import li.vin.net.TimeSeries;
import li.vin.net.User;
import li.vin.net.Vehicle;
import li.vin.net.Vinli;
import li.vin.net.VinliApp;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;

/**
 * Connect to the Vinli platform, log some basic info in a bunch of TextViews.
 * <br><br>
 * Uses <a href="https://github.com/vinli/android-net">the Vinli Net SDK</a>, <a
 * href="https://github.com/ReactiveX/RxJava">RxJava</a>, and
 * <a href="http://jakewharton.github.io/butterknife/">Butter Knife</a>.
 */
public class NetDemoActivity extends AppCompatActivity {

  @Bind(R.id.first_name) TextView firstName;
  @Bind(R.id.last_name) TextView lastName;
  @Bind(R.id.email) TextView email;
  @Bind(R.id.phone) TextView phone;
  @Bind(R.id.device_container) LinearLayout deviceContainer;
  @Bind(R.id.user_icon) CircularImageView userIconImageView;

  private boolean contentBound;
  private boolean signInRequested;
  private VinliApp vinliApp;
  private CompositeSubscription subscription;

  private final double MILES_PER_METER = 0.00062137;

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

    if(id == R.id.action_refresh){
      onRefreshClick();
      return true;
    }else if(id == R.id.action_sign_out){
      onSignOutClick();
      return true;
    }else{
      return super.onOptionsItemSelected(item);
    }
  }

  void onRefreshClick() {
    if (vinliApp != null && !isFinishing()) {
      subscribeAll();
    }
  }

  void onSignOutClick() {
    if (vinliApp != null && !isFinishing()) {
      signIn();
    }
  }

  /**
   * Load the VinliApp instance if possible - if not, proceed by either signing in or finishing the
   * Activity.
   */
  private void loadApp(Intent intent) {
    if (vinliApp == null) {
      vinliApp = intent == null
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
    Vinli.signIn(this, //
        getString(R.string.app_client_id), //
        getString(R.string.app_redirect_uri), //
        PendingIntent.getActivity(this, 0, new Intent(this, NetDemoActivity.class), 0));
  }

  /** Set content view and bind views. Only do this once. */
  private void setupContent() {
    if (contentBound) return;
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    contentBound = true;
  }

  /** Permissively subscribe to all data. Clean up beforehand if necessary. */
  private void subscribeAll() {
    // Clean up an existing subscriptions that are ongoing.
    cleanupSubscription();

    // Sanity check.
    if (vinliApp == null || !contentBound) return;

    // Gen composite subscription to hold all individual subscriptions to data.
    subscription = new CompositeSubscription();

    // Remove all views from device container - best practice would be to use an AdapterView for
    // this, such as ListView or RecyclerView, but for this simple example it's less verbose to
    // do it this way.
    deviceContainer.removeAllViews();

    // rx tip - use ConnectableObservables to minimize the number of network calls we need to make.
    // This allows each Observable that depends on User to take its data from the same source
    // rather than making unnecessary extra User lookups each time.
    ConnectableObservable<User> userObservable = vinliApp.currentUser().publish();

    userObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<User>() {
      @Override
      public void onCompleted() {

      }

      @Override
      public void onError(Throwable e) {

      }

      @Override
      public void onNext(User user) {
        Picasso.with(NetDemoActivity.this).load(user.image()).placeholder(R.drawable.default_user_image).into(userIconImageView);
      }
    });

    // Bind first name.
    subscribeToData(userObservable.map(new Func1<User, String>() {
      @Override
      public String call(User user) {
        return user.firstName();
      }
    }), firstName, getString(R.string.first_name));

    // Bind last name.
    subscribeToData(userObservable.map(new Func1<User, String>() {
      @Override
      public String call(User user) {
        return user.lastName();
      }
    }), lastName, getString(R.string.last_name));

    // Bind email.
    subscribeToData(userObservable.map(new Func1<User, String>() {
      @Override
      public String call(User user) {
        return user.email();
      }
    }), email, getString(R.string.email));

    // Bind phone.
    subscribeToData(userObservable.map(new Func1<User, String>() {
      @Override
      public String call(User user) {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if(Build.VERSION.SDK_INT >= 21){
          return PhoneNumberUtils.formatNumber(user.phone(), telephonyManager.getSimCountryIso());
        }else{
          return user.phone();
        }
      }
    }), phone, getString(R.string.phone));

    // Loop through each of the user's devices...
    subscription.add(vinliApp.devices() //
        .flatMap(Page.<Device>allItems()) //
        .observeOn(AndroidSchedulers.mainThread()) // Don't access Views off the UI thread.
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Toast.makeText(NetDemoActivity.this, //
                R.string.error_getting_devices, Toast.LENGTH_SHORT).show();
          }
        }) //
        .subscribe(new Subscriber<Device>() {
          @Override
          public void onCompleted() {
            // no-op
          }

          @Override
          public void onError(Throwable e) {
            // no-op
          }

          @Override
          public void onNext(Device device) {
            // Inflate device layout into device container. See above note about how using an
            // AdapterView would be better if this weren't just a naive example.
            View v = LayoutInflater.from(NetDemoActivity.this)
                .inflate(R.layout.device_layout, deviceContainer, false);
            TextView deviceName = (TextView) v.findViewById(R.id.device_name);
            TextView latestVehicle = (TextView) v.findViewById(R.id.latest_vehicle);
            TextView latestLocation = (TextView) v.findViewById(R.id.latest_location);
            TextView latestOdometer = (TextView) v.findViewById(R.id.odometer_estimate);
            CircularImageView deviceIcon = (CircularImageView) v.findViewById(R.id.device_icon);
            final Button setOdometerButton = (Button) v.findViewById(R.id.set_odometer_button);
            setOdometerButton.setClickable(false);
            deviceContainer.addView(v);

            Picasso.with(NetDemoActivity.this).load(device.icon()).placeholder(R.drawable.default_device_image).into(deviceIcon);

            setOdometerButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                Button button = (Button) v;
                presentSetOdometerModal((Vehicle) button.getTag(), false);
              }
            });

            // Bind device name.
            subscribeToData(Observable.just(device.name()), deviceName,
                getString(R.string.device_name));

            Observable<Vehicle> vehicleObservable = device.latestVehicle();

            // Bind latest vehicle info.
            subscribeToData(vehicleObservable.map(new Func1<Vehicle, String>() {
              @Override
              public String call(Vehicle vehicle) {
                if (vehicle == null) return getString(R.string.none);

                setOdometerButton.setTag(vehicle);
                setOdometerButton.setClickable(true);

                return getNameForVehicle(vehicle);
              }
            }), latestVehicle, getString(R.string.latest_vehicle));

            subscribeToData(vehicleObservable.flatMap(new Func1<Vehicle, Observable<TimeSeries<Odometer>>>() {
              @Override
              public Observable<TimeSeries<Odometer>> call(Vehicle vehicle) {
                return vehicle.odometerReports();
              }
            }).map(new Func1<TimeSeries<Odometer>, String>() {
              @Override
              public String call(TimeSeries<Odometer> odometerTimeSeries) {
                List<Odometer> odometerList = odometerTimeSeries.getItems();
                if (odometerList.size() > 0) {
                  Log.e("@@@@@", "Size: " + odometerList.size());
                  Odometer odometer = odometerList.get(0);
                  return String.format("%.2f miles", odometer.reading() * MILES_PER_METER);
                } else {
                  return getResources().getString(R.string.none);
                }
              }
            }), latestOdometer, getResources().getString(R.string.odometer_estimate));

            // Bind latest location info.
            subscribeToData(device.latestlocation().map(new Func1<Location, String>() {
              @Override
              public String call(Location location) {
                if (location == null) return getString(R.string.none);
                return location.coordinate().lat() + ", " + location.coordinate().lon();
              }
            }), latestLocation, getString(R.string.latest_location));
          }
        }));

    // Don't forget to connect the ConnectableObservable, or nothing will happen!
    userObservable.connect();
  }

  /**
   * Create a subscription that updates the given TextView with the given label with the string
   * value of whatever data is returned by the given Observable.
   */
  private <T> void subscribeToData(Observable<T> observable, final TextView view,
      final String label) {
    subscription.add(observable //
        .observeOn(AndroidSchedulers.mainThread()) // Don't access Views off the UI thread.
        .doOnSubscribe(new Action0() {
          @Override
          public void call() {
            view.setText(Html.fromHtml(String.format(getString(R.string.waiting_fmt), label)));
          }
        }) //
        .subscribe(new Subscriber<T>() {
          @Override
          public void onCompleted() {
            // no-op
          }

          @Override
          public void onError(Throwable e) {
            view.setText(Html.fromHtml(String.format(getString(R.string.error_fmt), label)));
          }

          @Override
          public void onNext(T val) {
            view.setText(Html.fromHtml(String.format(getString(R.string.success_fmt), label, val)));
          }
        }));
  }

  /** Unsubscribe all. Need to call this to clean up rx resources. */
  private void cleanupSubscription() {
    if (subscription != null) {
      if (!subscription.isUnsubscribed()) subscription.unsubscribe();
      subscription = null;
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

  private void presentSetOdometerModal(final Vehicle vehicle, boolean formatError){
    if(vehicle == null){
      Toast.makeText(NetDemoActivity.this, getString(R.string.null_vehicle_toast_message), Toast.LENGTH_SHORT).show();
      return;
    }

    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
    dialogBuilder.setTitle(getNameForVehicle(vehicle));

    if(formatError){
      dialogBuilder.setMessage("Please enter a valid number for the odometer reading for your car:");
    }else{
      dialogBuilder.setMessage("Please enter an odometer reading for you car:");
    }

    LinearLayout dialogLayout = (LinearLayout) LayoutInflater.from(NetDemoActivity.this).inflate(R.layout.odometer_picker, null, false);
    final EditText odometerReading = (EditText) dialogLayout.findViewById(R.id.reading_field);
    final NumberPicker unitPicker = (NumberPicker) dialogLayout.findViewById(R.id.unit_picker);
    odometerReading.getBackground().setColorFilter(getResources().getColor(R.color.button_dark_gray), PorterDuff.Mode.SRC_ATOP);
    odometerReading.setRawInputType(InputType.TYPE_CLASS_NUMBER);
    unitPicker.setMinValue(0);
    unitPicker.setMaxValue(2);
    unitPicker.setDisplayedValues(new String[]{"km", "m", "mi"});
    dialogBuilder.setView(dialogLayout);

    dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        String inputText = odometerReading.getText().toString();
        if(inputText.length() > 0){
          try {
            Double inputDouble = Double.parseDouble(inputText);
            DistanceUnit unit;
            switch (unitPicker.getValue()) {
              case 0:
                unit = DistanceUnit.KILOMETERS;
                break;
              case 1:
                unit = DistanceUnit.METERS;
                break;
              case 2:
                unit = DistanceUnit.MILES;
                break;
              default:
                unit = DistanceUnit.MILES;
                break;
            }
            Odometer.create()
                .reading(inputDouble).unit(unit)
                .vehicleId(vehicle.id())
                .save().subscribe(new Subscriber<Odometer>() {
              @Override
              public void onCompleted() {

              }

              @Override
              public void onError(Throwable e) {

              }

              @Override
              public void onNext(Odometer odometer) {
                Toast.makeText(NetDemoActivity.this, getString(R.string.successful_odometer_creation), Toast.LENGTH_SHORT).show();
              }
            });
          } catch (NumberFormatException e) {
            presentSetOdometerModal(vehicle, true);
          }
        }else{
          presentSetOdometerModal(vehicle, true);
        }
      }
    });

    dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
      }
    });

    AlertDialog dialog = dialogBuilder.show();

    int titleDividerId = getResources().getIdentifier("titleDivider", "id", "android");
    View titleDivider = dialog.findViewById(titleDividerId);
    if(titleDivider != null){
      titleDivider.setBackgroundColor(getResources().getColor(R.color.button_dark_gray));
    }
    int textViewId = getResources().getIdentifier("alertTitle", "id", "android");
    TextView titleView = (TextView) dialog.findViewById(textViewId);
    if(titleView != null){
      titleView.setTextColor(getResources().getColor(R.color.button_dark_gray));
    }
  }

  private String getNameForVehicle(Vehicle vehicle){
    if(vehicle == null){
      return getString(R.string.unnamed_vehicle);
    }

    String vStr = String.format("%s %s %s", vehicle.year(), vehicle.make(), vehicle.model()).trim();

    if (vStr.isEmpty() || (vehicle.year() == null && vehicle.make() == null && vehicle.model() == null)){
      return getString(R.string.unnamed_vehicle);
    }

    return vStr;
  }
}
