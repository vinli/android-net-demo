package li.vin.netdemo;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import li.vin.net.Device;
import li.vin.net.Location;
import li.vin.net.Page;
import li.vin.net.StreamMessage;
import li.vin.net.StreamMessage.DataType;
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

  private boolean contentBound;
  private boolean signInRequested;
  private VinliApp vinliApp;
  private CompositeSubscription subscription;

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

  @OnClick(R.id.refresh)
  void onRefreshClick() {
    if (vinliApp != null && !isFinishing()) {
      subscribeAll();
    }
  }

  @OnClick(R.id.sign_out)
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
        return user.phone();
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
          public void onNext(final Device device) {
            // Inflate device layout into device container. See above note about how using an
            // AdapterView would be better if this weren't just a naive example.
            View v = LayoutInflater.from(NetDemoActivity.this)
                .inflate(R.layout.device_layout, deviceContainer, false);
            TextView deviceName = (TextView) v.findViewById(R.id.device_name);
            TextView latestVehicle = (TextView) v.findViewById(R.id.latest_vehicle);
            TextView latestLocation = (TextView) v.findViewById(R.id.latest_location);
            deviceContainer.addView(v);

            ConnectableObservable<StreamMessage> stream = device.stream().publish();

            subscription.add(stream //
                .flatMap(StreamMessage.onlyWithIntVal(DataType.RPM)) //
                .observeOn(AndroidSchedulers.mainThread()) //
                .subscribe(new Subscriber<Integer>() {
                  @Override
                  public void onCompleted() {

                  }

                  @Override
                  public void onError(Throwable e) {

                  }

                  @Override
                  public void onNext(Integer rpm) {
                    Log.e("TESTO", "stream RPM for " + device.name() + " : " + rpm);
                  }
                }));

            subscription.add(stream //
                .flatMap(StreamMessage.onlyWithIntVal(DataType.VEHICLE_SPEED)) //
                .observeOn(AndroidSchedulers.mainThread()) //
                .subscribe(new Subscriber<Integer>() {
                  @Override
                  public void onCompleted() {

                  }

                  @Override
                  public void onError(Throwable e) {

                  }

                  @Override
                  public void onNext(Integer vss) {
                    Log.e("TESTO", "stream VSS for " + device.name() + " : " + vss);
                  }
                }));

            subscription.add(stream //
                .flatMap(StreamMessage.onlyWithFloatVal(DataType.MASS_AIRFLOW)) //
                .observeOn(AndroidSchedulers.mainThread()) //
                .subscribe(new Subscriber<Float>() {
                  @Override
                  public void onCompleted() {

                  }

                  @Override
                  public void onError(Throwable e) {

                  }

                  @Override
                  public void onNext(Float maf) {
                    Log.e("TESTO", "stream MAF for " + device.name() + " : " + maf);
                  }
                }));

            subscription.add(stream //
                .flatMap(StreamMessage.onlyWithFloatVal(DataType.CALCULATED_ENGINE_LOAD)) //
                .observeOn(AndroidSchedulers.mainThread()) //
                .subscribe(new Subscriber<Float>() {
                  @Override
                  public void onCompleted() {

                  }

                  @Override
                  public void onError(Throwable e) {

                  }

                  @Override
                  public void onNext(Float load) {
                    Log.e("TESTO", "stream load% for " + device.name() + " : " + load);
                  }
                }));

            stream.connect();

            //subscription.add(device.stream()
            //    .observeOn(AndroidSchedulers.mainThread())
            //    .subscribe(new Subscriber<StreamMessage>() {
            //      @Override
            //      public void onCompleted() {
            //        Log.e("TESTO", "stream onCompleted for " + device.name());
            //      }
            //
            //      @Override
            //      public void onError(Throwable e) {
            //        Log.e("TESTO", "stream onError for " + device.name(), e);
            //      }
            //
            //      @Override
            //      public void onNext(StreamMessage message) {
            //        Log.e("TESTO", "stream onNext for " + device.name() + "------------------");
            //        Log.e("TESTO", "---------------------------------------------------------");
            //
            //        int rpm = message.intVal(StreamMessage.DataType.RPM, -1);
            //        if (rpm != -1) Log.e("TESTO", device.name() + " RPM: " + rpm);
            //
            //        int vss = message.intVal(StreamMessage.DataType.VEHICLE_SPEED, -1);
            //        if (vss != -1) Log.e("TESTO", device.name() + " VSS: " + vss);
            //
            //        Coordinate coord = message.coord();
            //        if (coord != null) Log.e("TESTO", device.name() + " coord: " + coord);
            //
            //        StreamMessage.AccelData accel = message.accel();
            //        if (accel != null) Log.e("TESTO", device.name() + " accel: " + accel);
            //
            //        String fss = message.rawVal("fuelSystemStatus");
            //        if (fss != null) Log.e("TESTO", device.name() + " fuelSystemStatus: " + fss);
            //
            //        Log.e("TESTO", "---------------------------------------------------------");
            //      }
            //    }));

            // Bind device name.
            subscribeToData(Observable.just(device.name()), deviceName,
                getString(R.string.device_name));

            // Bind latest vehicle info.
            subscribeToData(device.latestVehicle().map(new Func1<Vehicle, String>() {
              @Override
              public String call(Vehicle vehicle) {
                if (vehicle == null) return getString(R.string.none);
                String vStr = (vehicle.year() + " " + //
                    vehicle.make() + " " + //
                    vehicle.model()).trim();
                if (vStr.isEmpty()) return getString(R.string.unnamed_vehicle);
                return vStr;
              }
            }), latestVehicle, getString(R.string.latest_vehicle));

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
            view.setText(String.format(getString(R.string.waiting_fmt), label));
          }
        }) //
        .subscribe(new Subscriber<T>() {
          @Override
          public void onCompleted() {
            // no-op
          }

          @Override
          public void onError(Throwable e) {
            view.setText(String.format(getString(R.string.error_fmt), label));
          }

          @Override
          public void onNext(T val) {
            view.setText(String.format(getString(R.string.success_fmt), label, val));
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
}
