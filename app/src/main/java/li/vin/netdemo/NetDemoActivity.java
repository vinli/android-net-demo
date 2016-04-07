package li.vin.netdemo;

import android.app.PendingIntent;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import li.vin.net.Device;
import li.vin.net.Location;
import li.vin.net.Page;
import li.vin.net.User;
import li.vin.net.Vehicle;
import li.vin.net.VinliBaseActivity;
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
public class NetDemoActivity extends VinliBaseActivity {

  @Bind(R.id.first_name) TextView firstName;
  @Bind(R.id.last_name) TextView lastName;
  @Bind(R.id.email) TextView email;
  @Bind(R.id.phone) TextView phone;
  @Bind(R.id.device_container) LinearLayout deviceContainer;

  private boolean contentBound;
  private CompositeSubscription subscription;

  @Override
  protected void onResume() {
    super.onResume();

    if(!super.signedIn()){
      signIn();
    }else{
      setup();
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    if(!super.signedIn()){
      signIn();
    }else{
      setup();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    cleanupSubscription();
  }

  @OnClick(R.id.refresh)
  void onRefreshClick() {
    if (super.signedIn() && !isFinishing()) {
      subscribeAll();
    }
  }

  @OnClick(R.id.sign_out)
  void onSignOutClick() {
    if (super.signedIn() && !isFinishing()) {
      signIn();
    }
  }

  public void setup(){
    setupContent();
    subscribeAll();
  }

  public void signIn(){
    super.signIn(
        getString(R.string.app_client_id),
        getString(R.string.app_redirect_uri),
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
    if (!super.signedIn() || !contentBound) return;

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
          public void onNext(Device device) {
            // Inflate device layout into device container. See above note about how using an
            // AdapterView would be better if this weren't just a naive example.
            View v = LayoutInflater.from(NetDemoActivity.this)
                .inflate(R.layout.device_layout, deviceContainer, false);
            TextView deviceName = (TextView) v.findViewById(R.id.device_name);
            TextView latestVehicle = (TextView) v.findViewById(R.id.latest_vehicle);
            TextView latestLocation = (TextView) v.findViewById(R.id.latest_location);
            deviceContainer.addView(v);

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
}
